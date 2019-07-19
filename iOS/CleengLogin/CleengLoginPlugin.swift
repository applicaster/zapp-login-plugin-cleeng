//
//  CleengLoginPlugin.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/3/19.
//

import ZappPlugins
import ZappLoginPluginsSDK
import CAM

private let kCleengUserLoginToken = "CleengUserLoginToken"

typealias StoreID = String
typealias OfferID = String
@objc public class CleengLoginPlugin: NSObject, ZPLoginProviderUserDataProtocol, ZPAppLoadingHookProtocol, ZPScreenHookAdapterProtocol, ZPPluggableScreenProtocol {
    
    public var screenPluginDelegate: ZPPlugableScreenDelegate?
    private var accessChecker = AccessChecker()
    private var userToken: String?
    private var currentAvailableOfferIDs = [StoreID: OfferID]() // offerStoreID: OfferID
    
    private var publisherId = ""
    private var networkAdapter: CleengNetworkHandler!
    public var configurationJSON: NSDictionary?
    
    private var flow: CAMFlow = .no
    
    public var isFlowBlocker: Bool {
        return true
    }
    
    public required override init() {
        super.init()
        networkAdapter = CleengNetworkHandler(publisherID: "")
    }
    
    public required init(configurationJSON: NSDictionary?) {
        super.init()
        self.configurationJSON = configurationJSON
        if let id = configurationJSON?["cleeng_login_publisher_id"] as? String {
            publisherId = id
        }
        
        networkAdapter = CleengNetworkHandler(publisherID: publisherId)
        networkAdapter.errorMessage = errorMessage()
    }
    
    public required init?(pluginModel: ZPPluginModel, screenModel: ZLScreenModel, dataSourceModel: NSObject?) {
        super.init()
        
        let playableItems = dataSourceModel as? [ZPPlayable] ?? []
        flow = accessChecker.getCamFlow(for: playableItems.first?.extensionsDictionary as? [String: Any],
                                        isAuthenticated: isAuthenticated())
    }
    
    public required init?(pluginModel: ZPPluginModel, dataSourceModel: NSObject?) {
        super.init()
        
        let playableItems = dataSourceModel as? [ZPPlayable] ?? []
        flow = accessChecker.getCamFlow(for: playableItems.first?.extensionsDictionary as? [String: Any],
                                        isAuthenticated: isAuthenticated())
    }
    
    // MARK: - Private methods
    
    private func silentAuthorization(completion: @escaping () -> Void) {
        guard let savedLoginToken = UserDefaults.standard.string(forKey: kCleengUserLoginToken) else {
            completion()
            return
        }
        networkAdapter.extendToken(token: savedLoginToken, completion: { (result) in
            switch result {
            case .success(let data):
                _ = self.parseAuthTokensResponse(json: data)
            case .failure:
                break
            }
            completion()
        })
    }
    
    private func authorize(api: CleengAPI, completion: @escaping (CAM.Result<Void>) -> Void) {
        networkAdapter.authorize(apiRequest: api, completion: { (result) in
            switch result {
            case .success(let data):
                let isParsed = self.parseAuthTokensResponse(json: data)
                isParsed == true ? completion(.success) : completion(.failure(CleengError.authTokenNotParsed))
            case .failure(let error):
                completion(.failure(error))
            }
        })
    }
    
    private func purchaseItem(token: String,
                              offerId: String,
                              transactionId: String,
                              receiptData: String,
                              isRestored: Bool, completion: @escaping (ItemPurchasingResult) -> Void) {
        networkAdapter.purchaseItem(token: userToken, offerId: offerId,
                                    transactionId: transactionId, receiptData: receiptData,
                                    isRestored: false) { (result) in
            switch result {
            case .success:
                self.verifyOnCleeng(offerId: offerId, completion: completion)
            case .failure(let error):
                completion(.failure(error))
                return
            }
        }
    }
    
    private func verifyOnCleeng(offerId: String, completion: @escaping (ItemPurchasingResult) -> Void) {
        networkAdapter.subscriptions(token: userToken,
                                     byAuthId: 0,
                                     offers: [offerId],
                                     completion: { (result) in
            switch result {
            case .success(let data):
                let offer = self.parseCleengOffersResponse(json: data)
                guard let verifiedOffer = offer.first else {
                    completion(.failure(.serverError))
                    return
                }
                if let access = verifiedOffer.accessGranted, access {
                    self.accessChecker.userPermissionEntitlementsIds.insert(verifiedOffer.authID)
                    completion(.success)
                } else {
                    completion(.failure(.serverError))
                    return
                }
            case .failure(let error):
                completion(.failure(error))
                return
            }
        })
    }

    private func errorMessage() -> (ErrorCodes) -> String {
        let nonexistentAlertKey = "nonexistent_user_alert_text"
        let existingUserAlertKey = "existing_user_alert_text"
        let invalidCredentialsAlertKey = "invalid_credentials_alert_text"
        let defaultAlertKey = "default_alert_text"
        
        let nonexistentAlertMessage = configurationJSON?[nonexistentAlertKey] as? String ?? ""
        let existingUserAlertMessage = configurationJSON?[existingUserAlertKey] as? String ?? ""
        let invalidCredentialsAlertMessage = configurationJSON?[invalidCredentialsAlertKey] as? String ?? ""
        let defaultAlertMessage = configurationJSON?[defaultAlertKey] as? String ?? ""
        
        let result: (ErrorCodes) -> String = { errorCode in
            var errorMessage = ""
            
            switch errorCode {
            case .nonexistentUser:
                errorMessage = nonexistentAlertMessage
            case .existingUser:
                errorMessage = existingUserAlertMessage
            case .invalidCredentials:
                errorMessage = invalidCredentialsAlertMessage
            case .unknown:
                errorMessage = defaultAlertMessage
            }
            
            return errorMessage
        }
        
        return result
    }
    
    // MARK: - ZPAppLoadingHookProtocol
    
    public func executeAfterAppRootPresentation(displayViewController: UIViewController?,
                                                completion: (() -> Swift.Void)?) {
        silentAuthorization(completion: { () in
            self.executeAfterAppRootPresentationFlow(displayViewController: displayViewController,
                                                     completion: completion)
        })
    }
    
    private func executeAfterAppRootPresentationFlow(displayViewController: UIViewController?,
                                                     completion: (() -> Swift.Void)?) {
        let flow = accessChecker.getStartupFlow(for: configurationJSON as? [String: Any],
                                                isAuthenticated: isAuthenticated())
        if flow != .no {
            guard let controller = displayViewController else {
                completion?()
                return
            }
            let contentAccessManager = ContentAccessManager(rootViewController: controller,
                                                            camDelegate: self,
                                                            camFlow: flow,
                                                            completion: { _ in completion?() })
            contentAccessManager.startFlow()
        } else {
            completion?()
        }
    }
    
    // MARK: - ZPLoginProviderUserDataProtocol
 
    public func isUserComply(policies: [String: NSObject]) -> Bool {
        let result = accessChecker.isUserComply(policies: policies, isAuthenticated: isAuthenticated())
        return result
    }
    
    public func isUserComply(policies: [String: NSObject], completion: @escaping (Bool) -> Void) {
        let result = accessChecker.isUserComply(policies: policies, isAuthenticated: isAuthenticated())
        completion(result)
    }
    
    // MARK: - ZPLoginProviderProtocol
    
    public func login(_ additionalParameters: [String: Any]?,
                      completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
    
        guard let controller = UIViewController.topmostViewController() else {
            assert(false, "No topmost controller")
            completion(.failed)
        }
        
        var flow = self.flow
        
        if additionalParameters != nil {
            flow = accessChecker.getCamFlow(for: additionalParameters, isAuthenticated: isAuthenticated())
        }
        
        let contentAccessManager = ContentAccessManager(rootViewController: controller,
                                                        camDelegate: self,
                                                        camFlow: flow) { (isCompleted) in
            (isCompleted == true) ? completion(.completedSuccessfully) : completion(.failed)
        }
        contentAccessManager.startFlow()
    }
    
    public func logout(_ completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
        completion(.completedSuccessfully)
    }
    
    public func isAuthenticated() -> Bool {
        return userToken != nil
    }
    
    public func isPerformingAuthorizationFlow() -> Bool {
        return networkAdapter.isPerformingAuthorizationFlow

    }
    
    public func getUserToken() -> String {
        return userToken ?? ""
    }
    
    // MARK: - JSON Response parsing
    
    private func parseCleengOffersResponse(json: Data) -> [CleengOffer] {
        guard let cleengOffers = try? JSONDecoder().decode([CleengOffer].self, from: json) else {
            return []
        }
        return cleengOffers
    }
    
    private func parseAuthTokensResponse(json: Data) -> Bool {
        guard let cleengTokens = try? JSONDecoder().decode([CleengToken].self, from: json) else {
            return false
        }
        for item in cleengTokens {
            if item.offerID.isEmpty {
                userToken = item.token // if offerID empty than we retrieve user token
                UserDefaults.standard.set(item.token, forKey: kCleengUserLoginToken)
            } else {
                if let authID = item.authID {
                    accessChecker.userPermissionEntitlementsIds.insert(authID) // if offerID !empty put
                                                                 //subscription token in dicrionary by authId
                }
            }
        }
        return true
    }
    
    // MARK: - ZPScreenHookAdapterProtocol
    
    public func requestScreenPluginPresentation(completion: @escaping (Bool) -> Void) {
        completion(false)
    }
    
    public func executeHook(presentationIndex: NSInteger,
                            dataDict: [String: Any]?,
                            taskFinishedWithCompletion: @escaping (Bool, NSError?, [String: Any]?) -> Void) {
        login(nil) { (operationStatus) in
            switch operationStatus {
            case .completedSuccessfully:
                taskFinishedWithCompletion(true, nil, nil)
            case .failed, .cancelled:
                taskFinishedWithCompletion(false, nil, nil)
            }
        }
    }
    
    //MARK: ZPPluggableScreenProtocol
    
    public func createScreen() -> UIViewController {
        return UIViewController()
    }
}

// MARK: - CAMDelegate

extension CleengLoginPlugin: CAMDelegate {
    
    public func getPluginConfig() -> [String: String] {
        return configurationJSON as? [String: String] ?? [:]
    }
    
    public func isPurchaseNeeded() -> Bool {
        return accessChecker.isPurchaseNeeded()
    }
    
    public func facebookLogin(userData: (email: String, userId: String),
                              completion: @escaping (LoginResult) -> Void) {
        let api = CleengAPI.loginWithFacebook(publisherID: publisherId,
                                              email: userData.email,
                                              facebookId: userData.userId)
//        authorize(api: api, completion: completion)
        authorize(api: api) { (result) in
            
        }
    }
    
    public func facebookSignUp(userData: (email: String, userId: String),
                               completion: @escaping (SignupResult) -> Void) {
        let api = CleengAPI.registerWithFacebook(publisherID: publisherId, email: userData.email,
                                                 facebookId: userData.userId)
        authorize(api: api, completion: completion)
    }
    
    public func login(authData: [String: String], completion: @escaping (LoginResult) -> Void) {
        let api = CleengAPI.login(publisherID: publisherId, email: authData["email"] ?? "",
                                  password: authData["password"] ?? "")
        authorize(api: api, completion: completion)
    }
    
    public func signUp(authData: [String: String], completion: @escaping (SignupResult) -> Void) {
        let api = CleengAPI.register(publisherID: publisherId, email: authData["email"] ?? "",
                                     password: authData["password"] ?? "")
        authorize(api: api, completion: completion)
    }
    
    public func resetPassword(data: [String: String], completion: @escaping (CAM.Result<Void>) -> Void) {
        networkAdapter.resetPassword(data: data, completion: { (result) in
            switch result {
            case .success:
                completion(.success)
            case .failure(let error):
                completion(.failure(error))
            }
        })
    }
    
    public func availableProducts(completion: @escaping (AvailableProductsResult) -> Void) {
        networkAdapter.subscriptions(token: userToken, byAuthId: 1,
                                     offers: accessChecker.currentItemEntitlementsIds, completion: { (result) in
            switch result {
            case .success(let data):
                let offers = self.parseCleengOffersResponse(json: data)
                self.currentAvailableOfferIDs = offers.reduce([String: String]()) { (dict, item) -> [String: String] in
                    var dict = dict
                    dict[item.appleProductID] = item.offerID
                    return dict
                }
                let storeIDs = Array(self.currentAvailableOfferIDs.keys)
                completion(.success(storeIDs))
            case .failure(let error):
                return
            }
        })
    }
    
    public func itemPurchased(purchasedItem: PurchasedProduct, completion: @escaping (PurchaseResult) -> Void) {
        guard let storeId = purchasedItem.product?.productIdentifier,
            let offerId = currentAvailableOfferIDs[storeId] else {
                return
        }
        let transactionId = purchasedItem.transaction?.transactionIdentifier
        self.purchaseItem(token: userToken ?? "",
                          offerId: offerId,
                          transactionId: transactionId ?? "",
                          receiptData: purchasedItem.receipt ?? "",
                          isRestored: false) { (result) in
            switch result {
            case .success:
                completion(.success)
            case .failure(let error):
                return
            }
        }
    }
    
    public func itemsRestored(restoredItems: [PurchasedProduct], completion: @escaping (PurchaseResult) -> Void) {
        let restoredOffers = restoredItems.reduce([(offerId: String, restoredItem: PurchasedProduct)]()) {
            (array, item) -> [(offerId: String, restoredItem: PurchasedProduct)] in
            var array = array
            guard let storeId = item.transaction?.payment.productIdentifier,
                let offerId = currentAvailableOfferIDs[storeId] else {
                    return array
            }
            array.append((offerId: offerId, restoredItem: item))
            return array
        }
        let dispatchGroup = DispatchGroup()
        var hasAccess = false
        
        var restoreError: Error?
        
        restoredOffers.forEach { (item) in
            dispatchGroup.enter()
            self.purchaseItem(token: userToken ?? "",
                              offerId: item.offerId,
                              transactionId: item.restoredItem.transaction?.transactionIdentifier ?? "",
                              receiptData: item.restoredItem.receipt ?? "",
                              isRestored: true) { (result) in
                switch result {
                case .success:
                    hasAccess = true
                case .failure(let error):
                    restoreError = error
                }
                dispatchGroup.leave()
            }
        }
        dispatchGroup.notify(queue: DispatchQueue.main) {
            hasAccess == true ? completion(.success) : completion(.failure(restoreError!))
        }
    }
}
