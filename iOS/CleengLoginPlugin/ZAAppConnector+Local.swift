//
//  ZAAppConnector+Local.swift
//  CleengPluginExample
//
//  Created by Yossi Avramov on 08/06/2018.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import ZappPlugins
import CAM

class MyConnector: NSObject, ZAAppDelegateConnectorLayoutsStylesProtocol, ZAAppDelegateConnectorURLProtocol {
    
    private var defaultStylesDelegate: ZAAppDelegateConnectorLayoutsStylesProtocol?
    private var defaultUrlDelegate: ZAAppDelegateConnectorURLProtocol?
    
    override init() {
        super.init()
        defaultStylesDelegate = ZAAppConnector.sharedInstance().layoutsStylesDelegate
        defaultUrlDelegate = ZAAppConnector.sharedInstance().urlDelegate
        
        ZAAppConnector.sharedInstance().layoutsStylesDelegate = self
        ZAAppConnector.sharedInstance().urlDelegate = self
    }
    
    // MARK: - ZAAppDelegateConnectorLayoutsStylesProtocol
    func stylesBundle() -> Bundle! {
        return Bundle.main
    }
    
    func zappLayoutsStylesBundle() -> Bundle! {
        return Bundle.main
    }
    
    func zappLayoutsStylesMappingDict() -> [AnyHashable: Any]! {
        return defaultStylesDelegate?.zappLayoutsStylesMappingDict()
    }
    
    func isZappLayoutsEnabled() -> Bool {
        return defaultStylesDelegate?.isZappLayoutsEnabled() ?? false
    }
    
    func zappLayoutViewController() -> AnyClass! {
        return defaultStylesDelegate?.zappLayoutViewController()
    }
    
    func defaultStatusBarStyle() -> UIStatusBarStyle {
        return defaultStylesDelegate?.defaultStatusBarStyle() ?? .default
    }
    
    func styleParams(byStyleName styleName: String!) -> [AnyHashable: Any]! {
        if let key = styleName, let style = CAMStyles(rawValue: key) {
            switch style {
            case .actionButton: return [
                "font": UIFont(name: "HelveticaNeue", size: 15)!,
                "color": UIColor.white
                ]
            case .alternativeLoginText: return [
                "font": UIFont(name: "HelveticaNeue", size: 15)!,
                "color": UIColor.white
                ]
            case .inputField: return [
                "font": UIFont(name: "HelveticaNeue", size: 14)!,
                "color": UIColor.black
                ]
            case .promtAction: return [
                "font": UIFont(name: "HelveticaNeue-Light", size: 14)!,
                "color": UIColor.white
                ]
            case .promtText: return [
                "font": UIFont(name: "HelveticaNeue-Bold", size: 14)!,
                "color": UIColor.white
                ]
            case .screenDescription: return [
                "font": UIFont(name: "HelveticaNeue-Medium", size: 16)!,
                "color": UIColor.white
                ]
            case .screenTitle: return [
                "font": UIFont(name: "HelveticaNeue", size: 26)!,
                "color": UIColor.white
                ]
            case .resetPassword: return [
                "font": UIFont(name: "HelveticaNeue-Light", size: 14)!,
                "color": UIColor.white
                ]
            case .separator: return [
                "font": UIFont(name: "HelveticaNeue-Light", size: 12)!,
                "color": UIColor.white
                ]
            case .alternateActionBannerColor: return ["color": UIColor(hex: "#FFFFFF50")]
            case .alertTitle:
                return [
                    "font": UIFont.boldSystemFont(ofSize: 25),
                    "color": UIColor.black
                ]
            case .alertDescription:
                return [
                    "font": UIFont.systemFont(ofSize: 20,
                                              weight: .light),
                    "color": UIColor.black
                ]
            default:
                return [
                    "font": UIFont(name: "HelveticaNeue-Light", size: 12)!,
                    "color": UIColor.black
                ]
            }
        } else {
            return defaultStylesDelegate?.styleParams?(byStyleName: styleName)
        }
    }
    
    func setViewStyle(_ view: UIView!, withKeys keys: [AnyHashable: Any]!) {
        if let styleKey = keys?[kZappLayoutStylesFontKey] as? String,
            let style = styleParams(byStyleName: styleKey),
            let font = style["font"] as? UIFont,
            let color = style["color"] as? UIColor {
            if let textView = view as? UITextView {
                textView.font = font
                textView.textColor = color
            } else if let textField = view as? UITextField {
                textField.font = font
                textField.textColor = color
            } else if let label = view as? UILabel {
                label.font = font
                label.textColor = color
            } else if let button = view as? UIButton {
                button.setTitleColor(color, for: .normal)
                button.titleLabel?.font = font
            }
        }
        
        if let color = keys?[kZappLayoutStylesBackgroundColorKey] as? String {
            let uiColor: UIColor?
            switch color {
            case "alternate_action_banner_bg_color": uiColor = UIColor(red: 0.0,
                                                                       green: 44.0/255.0,
                                                                       blue: 95.0/255.0,
                                                                       alpha: 0.84)
            default: uiColor = nil
            }
            
            if let uiColor = uiColor {
                view?.backgroundColor = uiColor
            }
        }
        
        if let imageKey = keys?[kZappLayoutStylesBackgroundImageKey] as? String,
            let imageURL = self.fileUrl(withName: imageKey, extension: nil) {
            if let imageView = view as? UIImageView {
                if let image = UIImage(contentsOfFile: imageURL.path) ?? UIImage(contentsOfFile: imageURL.absoluteString) {
                    imageView.image = image
                } else if let data = try? Data(contentsOf: imageURL), let image = UIImage(data: data, scale: 0) {
                    imageView.image = image
                }
            } else if let button = view as? UIButton {
                if let image = UIImage(contentsOfFile: imageURL.path) ?? UIImage(contentsOfFile: imageURL.absoluteString) {
                    button.setBackgroundImage(image, for: .normal)
                } else if let data = try? Data(contentsOf: imageURL), let image = UIImage(data: data, scale: 0) {
                    button.setBackgroundImage(image, for: .normal)
                }
            }
        }
        
//        if let textKey = keys?[kZappLayoutStylesLocalizationKey] as? String, let text = localizationString(byKey: textKey, defaultString: nil) {
//            if let textView = view as? UITextView {
//                textView.text = text
//            } else if let textField = view as? UITextField {
//                textField.text = text
//            } else if let label = view as? UILabel {
//                label.text = text
//            } else if let button = view as? UIButton {
//                button.setTitle(text, for: .normal)
//            }
//        }
    }
    
    func setLabelStyle(_ label: UILabel!, withKeys keys: [AnyHashable: Any]!) {
        if let styleKey = keys?[kZappLayoutStylesFontKey] as? String,
            let style = styleParams(byStyleName: styleKey),
            let font = style["font"] as? UIFont,
            let color = style["color"] as? UIColor {
            label.font = font
            label.textColor = color
        }
        
//        if let textKey = keys?[kZappLayoutStylesLocalizationKey] as? String, let text = localizationString(byKey: textKey, defaultString: nil) {
//            label.text = text
//        }
    }
    
    func setButtonStyle(_ button: UIButton!, withKeys keys: [AnyHashable: Any]!) {
        if let styleKey = keys?[kZappLayoutStylesFontKey] as? String,
            let style = styleParams(byStyleName: styleKey),
            let font = style["font"] as? UIFont,
            let color = style["color"] as? UIColor {
            button.setTitleColor(color, for: .normal)
            button.titleLabel?.font = font
        }
        
        if let imageKey = keys?[kZappLayoutStylesBackgroundImageKey] as? String,
            let imageURL = self.fileUrl(withName: imageKey, extension: nil) {
            if let image = UIImage(contentsOfFile: imageURL.path) ?? UIImage(contentsOfFile: imageURL.absoluteString) {
                button.setImage(image, for: .normal)
            } else if let data = try? Data(contentsOf: imageURL), let image = UIImage(data: data, scale: 0) {
                button.setImage(image, for: .normal)
            }
        }
        
//        if let textKey = keys?[kZappLayoutStylesLocalizationKey] as? String, let text = localizationString(byKey: textKey, defaultString: nil) {
//            button.setTitle(text, for: .normal)
//        }
    }
    
    // MARK: - ZAAppDelegateConnectorURLProtocol
    func appUrlSchemePrefix() -> String? {
        print("-------- \(String(describing: defaultUrlDelegate?.appUrlSchemePrefix()))")
        return defaultUrlDelegate?.appUrlSchemePrefix()
    }
    
    func fileUrl(withName fileName: String?, extension ext: String?) -> URL? {
        guard let name = fileName, let asset = CAMKeys(rawValue: name) else {
            return defaultUrlDelegate?.fileUrl(withName: fileName, extension: ext)
        }
        
        if let imageName = asset.rawValue as? String {
            let fileManager = FileManager.default
            let cacheDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            let url = cacheDirectory.appendingPathComponent("\(imageName)@\(Int(floor(UIScreen.main.scale)))x.png")
            let path = url.path
            
            guard fileManager.fileExists(atPath: path) else {
                guard
                    let image = UIImage(named: imageName),
                    let data = image.pngData()
                    else {
                        return defaultUrlDelegate?.fileUrl(withName: fileName, extension: ext)
                }
                
                fileManager.createFile(atPath: path, contents: data, attributes: nil)
                return url
            }
            
            return url
        } else {
            return defaultUrlDelegate?.fileUrl(withName: fileName, extension: ext)
        }
    }
}
