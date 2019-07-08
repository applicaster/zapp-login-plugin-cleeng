//
//  CleengOffer.swift
//  CleengLogin
//
//  Created by Roman Karpievich on 7/8/19.
//

import Foundation

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
