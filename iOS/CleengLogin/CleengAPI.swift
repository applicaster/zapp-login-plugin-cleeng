//
//  CleengAPI.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/6/19.
//

import Foundation
import CAM
import Alamofire

enum CleengAPI {
    case login(email: String?, password: String?)
    case loginWithFacebook(email: String?, facebookId: String?)
    case register(email: String?, password: String?)
    case registerWithFacebook(email: String?, facebookId: String?)
    case resetPassword(email: String?)
    case extendToken(token: String?)
    case subscriptions(token: String?, byAuthId: Int, offers: [String]?)
    case purchaseItem(offerId: String, token: String, transactionId: String,
                      receiptData: Data, isRestored: Bool)
    case purchaseItemUsingCode(offerId: String, token: String, reedeemCode: String)
    case restore(receipts: [RestorePurchaseData], token: String, receipt: String)
    case generateCustomerToken(email: String)
}
    
extension CleengAPI {
    var baseLink: String {
        return "https://applicaster-cleeng-sso.herokuapp.com/"
    }
    
    var path: String {
        switch self {
        case .login, .loginWithFacebook:
            return "login"
        case .register, .registerWithFacebook:
            return "register"
        case .resetPassword:
            return "passwordReset"
        case .extendToken:
            return "extendToken"
        case .subscriptions:
            return "subscriptions"
        case .purchaseItem, .purchaseItemUsingCode:
            return "subscription"
        case .restore:
            return "restoreSubscriptions"
        case .generateCustomerToken:
            return "generateCustomerToken"
            
        }
    }
    
    var url: URL {
        return URL(string: baseLink + path)!
    }
    
    var httpMethod: HTTPMethod {
        return .post
    }
    
    var params: [String: Any] {
        switch self {
        case .login(let email, let password):
            return ["email": email ?? "", "password": password ?? ""]
        case .loginWithFacebook(let email,
                                let facebookId):
            return ["email": email ?? "", "facebookId": facebookId ?? ""]
        case .register(let email, let password):
            let locale = Locale.current
            var country = ""
            if let countryCode = (locale as NSLocale).object(forKey: .countryCode) as? String {
                country = countryCode
            }
            return ["email": email ?? "",
                    "password": password ?? "",
                    "country": country,
                    "locale": "en_US",
                    "currency": "USD"]
        case .registerWithFacebook(let email, let facebookId):
            let locale = Locale.current
            var country = ""
            if let countryCode = (locale as NSLocale).object(forKey: .countryCode) as? String {
                country = countryCode
            }
            return ["email": email ?? "",
                    "facebookId": facebookId ?? "",
                    "country": country,
                    "locale": "en_US",
                    "currency": "USD"]
        case .resetPassword(let email):
            return ["email": email ?? ""]
        case .extendToken(let token):
            return ["token": token ?? ""]
        case .subscriptions(let token, let byAuthId, let offers):
            return ["token": token ?? "",
                    "byAuthId": byAuthId,
                    "offers": offers ?? [String]()]
        case .purchaseItem(let offerId, let token, let transactionId, let receiptData, let isRestored):
            let receiptInfo: [String: Any] = [
                "transactionId": transactionId,
                "receiptData": receiptData.base64EncodedString()
            ]
            let params: [String: Any] = [
                "appType": "ios",
                "receipt": receiptInfo,
                "offerId": offerId,
                "token": token,
                "isRestored": isRestored
            ]
            return params
        case .purchaseItemUsingCode(let offerId, let token, let couponCode):
            return [
                "appType": "ios",
                "offerId": offerId,
                "token": token,
                "couponCode": couponCode
            ]
        case .restore(let restoreData, let userToken, let receipt):
            let receipts: [[String: String]] = restoreData.map({["productId": $0.productId,
                                                                 "transactionId": $0.transactionId]})
            
            return ["appType": "ios",
                    "receipts": receipts,
                    "token": userToken,
                    "receiptData": receipt]
        case .generateCustomerToken(let email):
            return ["email": email]
        }
    }
}
