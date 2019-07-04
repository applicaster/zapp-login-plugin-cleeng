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

struct CleengToken: Codable {
    let offerID, token: String
    let authID: String?
    
    enum CodingKeys: String, CodingKey {
        case offerID = "offerId"
        case token
        case authID = "authId"
    }
}

typealias CleengTokens = [CleengToken]

struct CleengOffer: Codable {
    let offerID, appleProductID, authID: String
    let accessGranted: Bool?
    
    enum CodingKeys: String, CodingKey {
        case offerID = "id"
        case appleProductID = "appleProductId"
        case authID = "authId"
        case accessGranted
    }
}

typealias CleengOffers = [CleengOffer]
