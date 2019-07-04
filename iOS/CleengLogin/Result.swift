//
//  CleengResult.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/20/19.
//

import Foundation
import Alamofire

enum CleengError: Error {
    case requestError(Data)
    case networkError(AFError)
    case serverError
    
    var description: String {
        var result = ""
        
        switch self {
        case .requestError(let data):
            result = parseErrorResponse(from: data)?.message ?? "Server Error"
        case .networkError(let error):
            result = error.localizedDescription
        case .serverError:
            result = "Server error"
        }
        
        return result
    }
    
    private func parseErrorResponse(from json: Data) -> ServerError? {
        guard let serverError = try? JSONDecoder().decode(ServerError.self,
                                                          from: json) else {
            return nil
        }
        
        return serverError
    }
}

enum Result<Success, Failure> {
    case success(Success)
    case failure(Failure)
}

extension Result where Success == Void {
    static var success: Result {
        return .success(())
    }
}

extension Result where Failure == Void {
    static var failure: Result {
        return .failure(())
    }
}

typealias CleengAPIResult = Result<Data, CleengError>
typealias SilentLoginResult = Result<Void, Void>
