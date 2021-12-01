import Foundation
import React

@objc(MyEventEmitter)
class MyEventEmitter: RCTEventEmitter {

    public static var shared:MyEventEmitter?
    
    private var hasListeners = false

    override init() {
        super.init()
        MyEventEmitter.shared = self
    }

    override func supportedEvents() -> [String]! {
        return [
            "watchLocation",
        ]
    }
    
    override func startObserving() {
        hasListeners = true
    }
    
    override func stopObserving() {
        hasListeners = false
    }
    
    func locationReceived(location: Any) {
        if (hasListeners) {
            self.sendEvent(withName: "watchLocation", body: location)
        }
    }
}
