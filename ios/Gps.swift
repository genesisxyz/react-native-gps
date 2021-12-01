import Foundation
import CoreLocation
import CoreMotion

@objc(Gps)
class Gps: NSObject, CLLocationManagerDelegate {
    
    var locationManager: CLLocationManager? = nil
    var activityManager: CMMotionActivityManager? = nil
    
    func initializeLocationManager() -> CLLocationManager {
        let manager = locationManager ?? CLLocationManager()
        manager.delegate = self
        manager.requestAlwaysAuthorization()
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.distanceFilter = 50
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.activityType = .other
        return manager
    }
    
    func initializeActivityManager() -> CMMotionActivityManager {
        let manager = activityManager ?? CMMotionActivityManager()
        return manager
    }
    
    @objc(setOptions:)
    func setOptions(options: NSDictionary) -> Void {
        
    }
    
    @objc(startGpsService:withRejecter:)
    func startGpsService(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    @objc(stopGpsService:withRejecter:)
    func stopGpsService(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    @objc(startLocationUpdates)
    func startLocationUpdates() -> Void {
        let background = DispatchQueue.main
        background.sync {
            locationManager = initializeLocationManager();
            locationManager?.startUpdatingLocation()
        }
    }
    
    @objc(startGeofenceUpdates)
    func startGeofenceUpdates() -> Void {
        
    }
    
    @objc(startActivityRecognitionUpdates)
    func startActivityRecognitionUpdates() -> Void {
        
    }
    
    @objc(stopLocationUpdates)
    func stopLocationUpdates() -> Void {
        locationManager?.stopUpdatingLocation()
    }
    
    @objc(stopGeofenceUpdates)
    func stopGeofenceUpdates() -> Void {
        
    }
    
    @objc(stopActivityRecognitionUpdates)
    func stopActivityRecognitionUpdates() -> Void {
        
    }
    
    @objc(requestLocationPermissions:withRejecter:)
    func requestLocationPermissions( resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    @objc(requestActivityPermissions:withRejecter:)
    func requestActivityPermissions(resolve:RCTPromiseResolveBlock,r eject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    @objc(lastLocation:withRejecter:)
    func lastLocation(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(false)
    }
    
    @objc(addGeofences:withResolver:withRejecter:)
    func addGeofences(geofences:Array<[String: AnyObject]>, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    @objc(removeGeofences:withResolver:withRejecter:)
    func removeGeofences(geofencesIds:Array<String>, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    // MARK: CLLocationManagerDelegate
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {

        if (manager.location == nil) {
            return
        }
        
        let location = manager.location!
            
        let locationDict: [String: Any] = [
            "latitude": location.coordinate.latitude,
            "longitude": location.coordinate.longitude,
            "speed": location.speed,
            "accuracy": location.horizontalAccuracy,
            "altitude": location.altitude,
            "bearing": location.course,
            "time": location.timestamp,
            "isFromMockProvider": false,
        ]
        
        MyEventEmitter.shared?.locationReceived(location: locationDict)
    }
}
