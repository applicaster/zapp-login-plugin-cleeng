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

class ViewController: UIViewController {

    private var pluggableLogin: ZPLoginProviderProtocol?
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    @IBAction private func start() {
        let authKey = "requires_authentication"
        let entitlementsKey = "ds_product_ids"
        let item = Playable()
        item.extensionsDictionary = [authKey: true, entitlementsKey: ["48"]]
        let login = ZAAppConnector.sharedInstance().pluginsDelegate?.loginPluginsManager?.createWithUserData()
        pluggableLogin = login
        if let login = login {
            let params = ["playable_items": NSArray(array: [item])]
            login.login(["cleeng_login_start_with_action": "sign_in", "playable_items": [item]], completion: { _ in })
        }
    }
}
