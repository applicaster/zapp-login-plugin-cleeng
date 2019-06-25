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
}

enum Result<Success> {
    case success(Success)
    case failure(CleengError)
}

extension Result where Success == Void {
    static var success: Result {
        return .success(())
    }
}

typealias CleengResult = Result<Data>
