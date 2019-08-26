//
//  LoginErrors.swift
//  LoginError
//
//  Created by Roman Karpievich on 7/4/19.
//

import Foundation

class RequestError: Error {
    let errorCode: ErrorCodes
    var message: String = ""
    
    init(from code: ErrorCodes, with message: String) {
        self.errorCode = code
        self.message = message
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

enum CleengError: Error, LocalizedError {
    case requestError(RequestError)
    case networkError(Error)
    case serverError(RequestError)
    case authTokenNotParsed(RequestError)
    case serverDoesntVerifyPurchase(RequestError)
    
    public var errorDescription: String? {
        switch self {
        case .requestError(let error):
            return NSLocalizedString(error.message, comment: "")
        case .networkError(let error):
            return NSLocalizedString(error.localizedDescription, comment: "")
        case .serverError(let error):
            return NSLocalizedString(error.message, comment: "")
        case .authTokenNotParsed(let error):
            return NSLocalizedString(error.message, comment: "")
        case .serverDoesntVerifyPurchase(let error):
            return NSLocalizedString(error.message, comment: "")
        }
    }
}
