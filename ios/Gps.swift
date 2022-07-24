import Foundation

enum ActivityRecognitionType: Int {
    case InVechicle = 0
    case OnBicycle = 1
    case OnFoot = 2
    case Still = 3
    case Unknown = 4
    case Tilting = 5
    case Walking = 6
    case Running = 7
}

enum GeofenceTransition: Int {
    case Enter = 1
    case Exit = 2
    case Dwell = 3
}

@objc(Gps)
public class Gps: NSObject {

    @objc static func requiresMainQueueSetup() -> Bool {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(didFinishLaunchingNotification),
            name: UIApplication.didFinishLaunchingNotification,
            object: nil
        );
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(willTerminateNotification),
            name: UIApplication.willTerminateNotification,
            object: nil
        );
        
        return false
    }
    
    @objc static func willTerminateNotification(_ notification: Notification) {
        debugPrint("Gps willTerminateNotification")
        Location.shared.createRegion()
    }
    
    @objc static func didFinishLaunchingNotification(_ notification: Notification) {
        debugPrint("Gps didFinishLaunchingNotification")
        if let _ = notification.userInfo?[UIApplication.LaunchOptionsKey.location] {
            Location.shared.startLocationUpdates()
            Location.shared.startActivityRecognitionUpdates()
        }
    }

    @objc(setOptions:)
    func setOptions(options: [String: AnyObject]) -> Void {
        Location.shared.setOptions(options: options)
    }

    @objc(startGpsService:withRejecter:)
    func startGpsService(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        Location.shared.startGpsService(resolve: resolve, reject: reject)
    }

    @objc(stopGpsService:withRejecter:)
    func stopGpsService(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        Location.shared.stopGpsService(resolve: resolve, reject: reject)
    }

    @objc(startLocationUpdates)
    func startLocationUpdates() -> Void {
        Location.shared.startLocationUpdates()
    }

    @objc(startGeofenceUpdates)
    func startGeofenceUpdates() -> Void {
        Location.shared.startGeofenceUpdates()
    }

    @objc(startActivityRecognitionUpdates)
    func startActivityRecognitionUpdates() -> Void {
        Location.shared.startActivityRecognitionUpdates()
    }

    @objc(stopLocationUpdates)
    func stopLocationUpdates() -> Void {
        Location.shared.stopLocationUpdates()
    }

    @objc(stopGeofenceUpdates)
    func stopGeofenceUpdates() -> Void {
        Location.shared.stopGeofenceUpdates()
    }

    @objc(stopActivityRecognitionUpdates)
    func stopActivityRecognitionUpdates() -> Void {
        Location.shared.stopActivityRecognitionUpdates()
    }

    @objc(requestLocationPermissions:withRejecter:)
    func requestLocationPermissions(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        Location.shared.requestLocationPermissions(resolve: resolve, reject: reject)
    }

    @objc(requestActivityPermissions:withRejecter:)
    func requestActivityPermissions(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        Location.shared.requestActivityPermissions(resolve: resolve, reject: reject)
    }

    @objc(lastLocation:withRejecter:)
    func lastLocation(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        Location.shared.lastLocation(resolve: resolve, reject: reject)
    }

    @objc(addGeofences:withResolver:withRejecter:)
    func addGeofences(geofences:Array<[String: AnyObject]>, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        Location.shared.addGeofences(geofences: geofences, resolve: resolve, reject: reject)
    }

    @objc(removeGeofences:withResolver:withRejecter:)
    func removeGeofences(geofencesIds:Array<String>, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }

    @objc(startGooglePlacesAutocompleteSession)
    func startGooglePlacesAutocompleteSession() {
        Location.shared.startGooglePlacesAutocompleteSession()
    }

    @objc(findAutocompletePredictions:withOptions:withResolver:withRejecter:)
    func findAutocompletePredictions(query: String, options: [String: AnyObject], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        Location.shared.findAutocompletePredictions(query: query, options: options, resolve: resolve, reject: reject)
    }

    @objc(getPredictionByPlaceId:withResolver:withRejecter:)
    func getPredictionByPlaceId(placeId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        Location.shared.getPredictionByPlaceId(placeId: placeId, resolve: resolve, reject: reject)
    }
}
