//
//  CleengOffer.swift
//  CleengLogin
//
//  Created by Roman Karpievich on 7/8/19.
//

import Foundation

struct CleengOffer: Codable {
    let offerID: String
    let period: String?
    let freeDays: String?
    let appleProductID: String
    let authID: String
    let accessGranted: Bool?
    let accessToTags: [String]?
    
    enum CodingKeys: String, CodingKey {
        case offerID = "id"
        case period
        case freeDays
        case appleProductID = "appleProductId"
        case authID = "authId"
        case accessGranted
        case accessToTags
    }
}
