//
//  Result+Success.swift
//  CleengLogin
//
//  Created by Roman Karpievich on 11/16/19.
//

import Foundation

extension Result where Success == Void {
    static var success: Result {
        return .success(())
    }
}
