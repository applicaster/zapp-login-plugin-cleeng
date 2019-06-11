//
//  CleengNetworkHandler.swift
//  Alamofire
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
    
    func login(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.login(publisherID: publisherID, email: authData["email"], password: authData["password"])
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
    
    func loginWithFacebook(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.loginWithFacebook(publisherID: publisherID, email: authData["email"],
                                              facebookId: authData["facebookId"])
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
    
    func register(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.register(publisherID: publisherID, email: authData["email"], password: authData["password"])
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
    
    func registerWithFacebook(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.registerWithFacebook(publisherID: publisherID, email: authData["email"],
                                                 facebookId: authData["facebookId"])
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
    
    func resetPassword(data: [String: String], completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.resetPassword(publisherID: publisherID, email: data["email"])
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
    
    func extendToken(token: String) {
        let api = CleengAPI.extendToken(publisherID: publisherID, token: token)
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
    
    func subscriptions(token: String?, byAuthId: Int, offers: [String]?) { //0 - by offers, 1 by auth ids
        let api = CleengAPI.subscriptions(publisherID: publisherID, token: token, byAuthId: byAuthId, offers: offers)
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
    
    func subscription(transactionId: String?, receiptData: String?, token: String?, offerId: String?, isRestored: Bool, couponCode: String?) {
        let api = CleengAPI.subscription(publisherID: publisherID, transactionId: transactionId,
                                         receiptData: receiptData, token: token, offerId: offerId, isRestored: isRestored, couponCode: couponCode)
        Alamofire.request(api.url, method: api.httpMethod, parameters: api.params).responseJSON { response in
            print(response)
        }
    }
}
