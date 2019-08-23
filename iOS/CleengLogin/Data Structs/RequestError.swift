//
//  LoginErrors.swift
//  LoginError
//
//  Created by Roman Karpievich on 7/4/19.
//

import Foundation

class RequestError: Error, LocalizedError {
    let errorCode: ErrorCodes
    var message: String = ""
    
    init(from code: ErrorCodes, with message: String) {
        self.errorCode = code
        self.message = message
    }
    
    // MARK: - LocalizedError
    
    var errorDescription: String? {
        return NSLocalizedString(message, comment: "")
    }
}

enum ErrorCodes: Int, Codable {
    case nonexistentUser = 10
    case existingUser = 13
    case invalidCredentials = 15
    case unknown = 0
    
    init(rawValue: Int) {
        switch rawValue {
        case 10:
            self = .nonexistentUser
        case 13:
            self = .existingUser
        case 15:
            self = .invalidCredentials
        default:
            self = .unknown
        }
    }
    
    // MARK: - Codable
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let code = try container.decode(Int.self, forKey: .code)
        
        self.init(rawValue: code)
    }
    
    private enum CodingKeys: String, CodingKey {
        case code
        case message
    }
}

enum CleengError: Error {
    case requestError(RequestError)
    case networkError(Error)
    case serverError
    case authTokenNotParsed
    case serverDoesntVerifyPurchase(RequestError)
}
