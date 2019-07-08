//
//  CleengToken.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/18/19.
//

import Foundation

struct CleengToken: Codable {
    let offerID: String
    let token: String
    let authID: String?
    
    enum CodingKeys: String, CodingKey {
        case offerID = "offerId"
        case token
        case authID = "authId"
    }
}
