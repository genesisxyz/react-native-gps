import Foundation
import CoreLocation
import CoreMotion
import GooglePlaces
import GoogleMapsBase

@objc(Gps)
class Gps: NSObject, CLLocationManagerDelegate {
    
    var locationManager: CLLocationManager? = nil
    var activityManager: CMMotionActivityManager? = nil
    
    var sessionToken: GMSAutocompleteSessionToken? = nil
    
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
        if let location = locationManager?.location {
            let locationDict: [String: Any] = [
                "latitude": location.coordinate.latitude,
                "longitude": location.coordinate.longitude,
                "speed": location.speed,
                "accuracy": location.horizontalAccuracy,
                "altitude": location.altitude,
                "bearing": location.course,
                "time": location.timestamp,
                "isFromMockProvider": false,
            ];
            
            resolve(locationDict)
        } else {
            resolve(nil)
        }
    }
    
    @objc(addGeofences:withResolver:withRejecter:)
    func addGeofences(geofences:Array<[String: AnyObject]>, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    @objc(removeGeofences:withResolver:withRejecter:)
    func removeGeofences(geofencesIds:Array<String>, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }
    
    @objc(startGooglePlacesAutocompleteSession)
    func startGooglePlacesAutocompleteSession() {
        sessionToken = GMSAutocompleteSessionToken.init()
    }
    
    @objc(findAutocompletePredictions:withOptions:withResolver:withRejecter:)
    func findAutocompletePredictions(query: String, options: [String: AnyObject], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {

        let filter = GMSAutocompleteFilter()
        filter.type = .address
        filter.country = "IT"
        
        let northEastBoundsDict = options["northEastBounds"]
        let northEastBound = CLLocationCoordinate2D(latitude: northEastBoundsDict?["latitude"] as! CLLocationDegrees, longitude: northEastBoundsDict?["longitude"] as! CLLocationDegrees)
        
        let southWestBoundsDict = options["southWestBounds"]
        let southWestBounds = CLLocationCoordinate2D(latitude: southWestBoundsDict?["latitude"] as! CLLocationDegrees, longitude: southWestBoundsDict?["longitude"] as! CLLocationDegrees)
        
        let bounds = GMSCoordinateBounds(coordinate: northEastBound, coordinate: southWestBounds)
        
        let background = DispatchQueue.main
        background.sync {
            let placesClient = GMSPlacesClient.shared()
            
            placesClient.findAutocompletePredictions(fromQuery: query, bounds: bounds, boundsMode: GMSAutocompleteBoundsMode.bias, filter: filter, sessionToken: sessionToken, callback: { (results, error) in
                if let _ = error {
                    resolve([])
                }
                
                if let results = results {
                    let resultsDict = results.map { prediction in
                        [
                            "attributedFullText": prediction.attributedFullText.string,
                            "attributedPrimaryText": prediction.attributedPrimaryText.string,
                            "attributedSecondaryText": prediction.attributedSecondaryText?.string ?? "",
                            "placeID": prediction.placeID,
                            "types": prediction.types,
                        ]
                    }
                    
                    resolve(resultsDict)
                } else {
                    resolve([])
                }
            })
        }
    }
    
    @objc(getPredictionByPlaceId:withResolver:withRejecter:)
    func getPredictionByPlaceId(placeId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let fields: GMSPlaceField = GMSPlaceField(
            rawValue:
                UInt(GMSPlaceField.name.rawValue) |
                UInt(GMSPlaceField.placeID.rawValue) |
                UInt(GMSPlaceField.coordinate.rawValue) |
                UInt(GMSPlaceField.formattedAddress.rawValue)
        )!

        let background = DispatchQueue.main
        background.sync {
            let placesClient = GMSPlacesClient.shared()
            
            placesClient.fetchPlace(fromPlaceID: placeId, placeFields: fields, sessionToken: nil, callback: { (place: GMSPlace?, error: Error?) in
              if let _ = error {
                  resolve(nil)
              }
              if let place = place {
                  let placeDict: [String: Any] = [
                    "name": place.name ?? "",
                    "placeID": place.placeID ?? "",
                    "formattedAddress": place.formattedAddress ?? "",
                    "coordinate": [
                        "latitude": place.coordinate.latitude,
                        "longitude": place.coordinate.longitude,
                    ],
                  ]
                  
                  resolve(placeDict)
              }
            })
        }
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
