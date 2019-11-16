//
//  CleengNetworkHandler.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/6/19.
//

import Foundation
import CAM
import Alamofire
import ApplicasterSDK

class CleengNetworkHandler {
    var isPerformingAuthorizationFlow = false
    let publisherID: String
    
    var errorMessage: (ErrorCodes) -> String = {_ in return ""}
    
    init(publisherID: String) {
        self.publisherID = publisherID
    }
    
    func authorize(apiRequest: CleengAPI, completion: @escaping (Swift.Result<Void, Error>) -> Void) {
        isPerformingAuthorizationFlow = true

        switch apiRequest {
        case .login, .loginWithFacebook, .register, .registerWithFacebook:
            performRequest(api: apiRequest) { (result) in
                self.isPerformingAuthorizationFlow = false
                
                switch result {
                case .success(let data):
                    let isParsed = (self.parseAuthTokensResponse(json: data).count > 0)
                    if isParsed {
                        completion(.success)
                    } else {
                        completion(.failure(CleengError.authTokenNotParsed))
                    }
                case .failure(let error):
                    completion(.failure(error))
                }
            }
        default:
            assert(false, "Wrong API passed to authorize")
        }
    }
    
    func resetPassword(data: [String: String], completion: @escaping (Swift.Result<Void, Error>) -> Void) {
        let api = CleengAPI.resetPassword(publisherID: publisherID, email: data["email"])
        performRequest(api: api) { (result) in
            switch result {
            case .success(_):
                completion(.success)
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    func extendToken(token: String, completion: @escaping (Swift.Result<[CleengToken], Error>) -> Void) {
        isPerformingAuthorizationFlow = true

        let api = CleengAPI.extendToken(publisherID: publisherID, token: token)
        performRequest(api: api) { (result) in
            self.isPerformingAuthorizationFlow = false
            
            switch result {
            case .success(let data):
                let cleengTokens = self.parseAuthTokensResponse(json: data)
                completion(.success(cleengTokens))
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    func subscriptions(token: String?, byAuthId: Int, offers: [String]?,
                       completion: @escaping (Swift.Result<Data, Error>) -> Void) { //0 - by offers, 1 by auth ids
        let api = CleengAPI.subscriptions(publisherID: publisherID,
                                          token: token,
                                          byAuthId: byAuthId,
                                          offers: offers)
        performRequest(api: api, completion: completion)
    }
    
    func purchaseItem(token: String?,
                      offerId: String,
                      transactionId: String,
                      receiptData: Data,
                      isRestored: Bool, completion:  @escaping (Swift.Result<Void, Error>) -> Void) {
        let api = CleengAPI.purchaseItem(publisherID: publisherID,
                                         offerId: offerId,
                                         token: token ?? "",
                                         transactionId: transactionId,
                                         receiptData: receiptData,
                                         isRestored: isRestored)
        performRequest(api: api) { (result) in
            switch result {
            case .success:
                self.verify(purchase: offerId, completion: { result in
                    switch result {
                    case .success:
                        completion(.success)
                    case .failure(let error):
                        completion(.failure(error))
                    }
                })
            case .failure(let error):
                completion(.failure(error))
                return
            }
        }
    }
    
    func restore(purchases: [RestorePurchaseData],
                 token: String,
                 receipt: String,
                 completion: @escaping (Swift.Result<Void, Error>) -> Void) {
        let api = CleengAPI.restore(publisherID: publisherID,
                                    receipts: purchases,
                                    token: token,
                                    receipt: receipt)
            performRequest(api: api) { (result) in
            switch (result) {
            case .success(let data):
                guard let restoredOffers = try? JSONDecoder().decode([RestoredCleengOffer].self,
                                                                     from: data) else {
                                                                        completion(.failure(CleengError.serverError))
                                                                        return
                }
                
                var restoreError: Error?
                var isRestoreAtLeastOneItem = false
                let group = DispatchGroup()
                
                let uniqueOffers = Dictionary(grouping: restoredOffers, by: { $0.offerId }).keys
                uniqueOffers.forEach({ (offerId) in
                    self.verify(purchase: offerId, completion: { result in
                        switch result {
                        case .success:
                            isRestoreAtLeastOneItem = true
                        case .failure(let error):
                            restoreError = error
                        }
                        group.leave()
                    })
                })
                
                group.notify(queue: .main, execute: {
                    if let error = restoreError, isRestoreAtLeastOneItem == false {
                        completion(.failure(error))
                    } else {
                        completion(.success(()))
                    }
                })
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    func performRequest(api: CleengAPI, completion: @escaping (Swift.Result<Data, Error>) -> Void) {
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params, encoding: JSONEncoding.default).responseJSON { (response) in
            switch response.result {
            case .success:
                guard let code = response.response?.statusCode, let data = response.data else {
                    completion(.failure(CleengError.serverError))
                    return
                }
                switch code {
                case 200..<300:
                    completion(.success(data))
                default:
                    let errorCode = (try? JSONDecoder().decode(ErrorCodes.self, from: data)) ?? .unknown
                    let errorMessage = self.errorMessage(errorCode)
                    let error = RequestError(from: errorCode,
                                           with: errorMessage)
                    completion(.failure(CleengError.requestError(error)))
                }
            case .failure(let error):
                completion(.failure(CleengError.networkError(error)))
            }
        }
    }
    
    private func verify(purchase offerId: String, completion: @escaping (Swift.Result<Void, Error>) -> Void) {
        let timerStartTime = Date()
        Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { timer in
            if Date().timeIntervalSince(timerStartTime) > 60 {
                timer.invalidate()
                let errorMessage = self.errorMessage(ErrorCodes.unknown)
                let error = RequestError(from: ErrorCodes.unknown, with: errorMessage)
                completion(.failure(CleengError.serverDoesntVerifyPurchase(error)))
                return
            }
            guard let userToken = CleengLoginPlugin.userToken else {
                timer.invalidate()
                completion(.failure(CleengError.authTokenNotParsed))
                return
            }
            self.extendToken(token: userToken, completion: { (result) in
                switch result {
                case .success(let cleengTokens):
                    let isOfferVerified = cleengTokens.contains(where: { (item) -> Bool in
                        return item.offerID == offerId
                    })
                    
                    if isOfferVerified {
                        timer.invalidate()
                        completion(.success)
                        return
                    }
                case .failure:
                    break
                }
            })
        }
    }
    
    private func parseAuthTokensResponse(json: Data) -> [CleengToken] {
        guard let cleengTokens = try? JSONDecoder().decode([CleengToken].self, from: json) else {
            return []
        }
        
        for item in cleengTokens {
            if item.offerID.isEmpty {
                CleengLoginPlugin.userToken = item.token // if offerID empty than we retrieve user token
                UserDefaults.standard.set(item.token, forKey: kCleengUserLoginToken)
            } else if let authID = item.authID {
                AccessChecker.userPermissionEntitlementsIds.insert(authID) // if offerID !empty put
                //subscription token in dicrionary by authId
                APAuthorizationManager.sharedInstance().setAuthorizationToken(item.token,
                                                                              withAuthorizationProviderID: authID) //set auth token for auth id. Need for applicaster player.
            }
        }
        
        return cleengTokens
    }
}
