//
//  JSONClasses.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/18/19.
//

import Foundation

struct ServerError: Codable {
    let code: Int
    let message: String
}
