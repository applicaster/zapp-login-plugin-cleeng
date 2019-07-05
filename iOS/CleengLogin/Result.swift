//
//  CleengResult.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/20/19.
//

import Foundation

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
