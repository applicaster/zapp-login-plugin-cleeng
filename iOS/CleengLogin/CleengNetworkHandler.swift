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
    var publisherID = ""
    
    init(publisherID: String) {
        self.publisherID = publisherID
    }
    
    func login(authData: [String: String], completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.login(publisherID: publisherID, email: authData["email"], password: authData["password"])
        performRequest(api: api, completion: completion)
    }
    
    func loginWithFacebook(email: String, facebookId: String, completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.loginWithFacebook(publisherID: publisherID, email: email,
                                              facebookId: facebookId)
        performRequest(api: api, completion: completion)
    }
    
    func register(authData: [String: String], completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.register(publisherID: publisherID, email: authData["email"], password: authData["password"])
        performRequest(api: api, completion: completion)
    }
    
    func registerWithFacebook(email: String, facebookId: String, completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.registerWithFacebook(publisherID: publisherID, email: email,
                                                 facebookId: facebookId)
        performRequest(api: api, completion: completion)
    }
    
    func resetPassword(data: [String: String], completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.resetPassword(publisherID: publisherID, email: data["email"])
        performRequest(api: api, completion: completion)
    }
    
    func extendToken(token: String, completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.extendToken(publisherID: publisherID, token: token)
        performRequest(api: api, completion: completion)
    }
    
    func subscriptions(token: String?, byAuthId: Int, offers: [String]?, completion: @escaping (CleengAPIResult) -> Void) { //0 - by offers, 1 by auth ids
        let api = CleengAPI.subscriptions(publisherID: publisherID, token: token, byAuthId: byAuthId, offers: offers)
        performRequest(api: api, completion: completion)
    }
    
    func subscription(transactionId: String?, receiptData: String?, token: String?, offerId: String?,
                      isRestored: Bool, couponCode: String?, completion: @escaping (CleengAPIResult) -> Void) {
        let api = CleengAPI.subscription(publisherID: publisherID, transactionId: transactionId,
                                         receiptData: receiptData, token: token, offerId: offerId, isRestored: isRestored, couponCode: couponCode)
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
                    completion(.failure(.requestError(data)))
                }
            case .failure(let error):
                if let error = error as? AFError {
                    completion(.failure(.networkError(error)))
                }
            }
        }
    }
}
