import Foundation
import React

@objc(MyEventEmitter)
class MyEventEmitter: RCTEventEmitter {

    public static var shared: MyEventEmitter?

    override init() {
        super.init()
        MyEventEmitter.shared = self
    }
    
    @objc override static func requiresMainQueueSetup() -> Bool {
        return false
    }

    override func supportedEvents() -> [String]! {
        return [
            "watchLocation",
            "watchActivity",
            "watchGeofence",
            "watchLocationPermissions",
            "watchActivityPermissions",
        ]
    }
    
    func watchLocationPermissions(status: Int32) {
        self.sendEvent(withName: "watchLocationPermissions", body: status)
    }
    
    func watchActivityPermissions(status: Int) {
        self.sendEvent(withName: "watchActivityPermissions", body: status)
    }
    
    func locationsReceived(locations: Array<[String: Any?]>) {
        self.sendEvent(withName: "watchLocation", body: locations)
    }
    
    func activityReceived(activity: [String: Any]) {
        self.sendEvent(withName: "watchActivity", body: activity)
    }
    
    func geofenceReceived(geofence: [String: Any]) {
        self.sendEvent(withName: "watchGeofence", body: geofence)
    }
}
