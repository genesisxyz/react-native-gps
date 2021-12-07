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
            "watchActivity",
        ]
    }
    
    override func startObserving() {
        hasListeners = true
    }
    
    override func stopObserving() {
        hasListeners = false
    }
    
    func locationReceived(location: [String: Any]) {
        if (hasListeners) {
            self.sendEvent(withName: "watchLocation", body: location)
        }
    }
    
    func activityReceived(activity: [String: Any]) {
        if (hasListeners) {
            self.sendEvent(withName: "watchActivity", body: activity)
        }
    }
}
