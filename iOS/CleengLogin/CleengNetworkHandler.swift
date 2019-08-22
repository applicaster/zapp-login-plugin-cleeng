//
//  CleengNetworkHandler.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/6/19.
//

import Foundation
import CAM
import Alamofire

class CleengNetworkHandler {
    var isPerformingAuthorizationFlow = false
    let publisherID: String
    
    var errorMessage: (ErrorCodes) -> String = {_ in return ""}
    
    init(publisherID: String) {
        self.publisherID = publisherID
    }
    
    func authorize(apiRequest: CleengAPI, completion: @escaping (CleengAPIResult) -> Void) {
        isPerformingAuthorizationFlow = true
        let authorizationCompletion: (CleengAPIResult) -> Void = { result in
            self.isPerformingAuthorizationFlow = false
            completion(result)
        }
        switch apiRequest {
        case .login, .loginWithFacebook, .register, .registerWithFacebook:
            performRequest(api: apiRequest, completion: authorizationCompletion)
        default:
            assert(false, "Wrong API passed to authorize")
        }
    }
    
    func resetPassword(data: [String: String], completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.resetPassword(publisherID: publisherID, email: data["email"])
        performRequest(api: api, completion: completion)
    }
    
    func extendToken(token: String, completion: @escaping (CleengAPIResult) -> Void) {
        isPerformingAuthorizationFlow = true
        let extendTokenCompletion: (CleengAPIResult) -> Void = { result in
            self.isPerformingAuthorizationFlow = false
            completion(result)
        }
        let api = CleengAPI.extendToken(publisherID: publisherID, token: token)
        performRequest(api: api, completion: extendTokenCompletion)
    }
    
    func subscriptions(token: String?, byAuthId: Int, offers: [String]?,
                       completion: @escaping (CleengAPIResult) -> Void) { //0 - by offers, 1 by auth ids
        let api = CleengAPI.subscriptions(publisherID: publisherID,
                                          token: token,
                                          byAuthId: byAuthId,
                                          offers: offers)
        performRequest(api: api, completion: completion)
    }
    
    func purchaseItem(token: String?, offerId: String, transactionId: String, receiptData: Data,
                      isRestored: Bool, completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.purchaseItem(publisherID: publisherID,
                                         offerId: offerId,
                                         token: token ?? "",
                                         transactionId: transactionId,
                                         receiptData: receiptData,
                                         isRestored: isRestored)
        performRequest(api: api, completion: completion)
    }
    
    func performRequest(api: CleengAPI, completion: @escaping (CleengAPIResult) -> Void) {
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { (response) in
            switch response.result {
            case .success:
                guard let code = response.response?.statusCode, let data = response.data else {
                    completion(.failure(.serverError))
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
                    completion(.failure(.requestError(error)))
                }
            case .failure(let error):
                completion(.failure(.networkError(error)))
            }
        }
    }
}
