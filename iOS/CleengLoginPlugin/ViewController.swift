//
//  ViewController.swift
//  CleengLoginPlugin
//
//  Created by Egor Brel on 6/3/19.
//  Copyright © 2019 Egor Brel. All rights reserved.
//

import UIKit
import ZappPlugins
import CleengLogin
import ZappLoginPluginsSDK
import ApplicasterSDK

class ViewController: UIViewController {

    private var pluggableLogin: ZPLoginProviderProtocol?
    private let myConnectot = MyConnector()
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    @IBAction private func start() {
        let item = TestableAPPurchasableItem(authIds: ["48"])
        let login = ZPLoginManager.sharedInstance.createWithUserData()
        pluggableLogin = login
        if let login = login {
            let params = ["playable_items": NSArray(array: [item])]
            login.isUserComply!(policies: params) { [weak login] (isComply) in
                if !isComply {
                    login?.login(["cleeng_login_start_with_action": "sign_in", "playable_items": [item]], completion: { _ in })
                }
            }
        }
        
    }

}

class TestableAPPurchasableItem: APPurchasableItem {
    private var authIds: [String] = []
    convenience init(authIds: [String]) {
        self.init()
        self.authIds = authIds
    }
    
    override var authorizationProvidersIDs: NSArray! {
        return authIds as NSArray
    }
    
    override func isLoaded() -> Bool {
        return true
    }
}
