import Foundation
import CoreLocation
import CoreMotion
import GooglePlaces
import GoogleMapsBase

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
class Gps: NSObject, CLLocationManagerDelegate {

    var locationManager: CLLocationManager? = nil
    var locationManagerForGeofencing: CLLocationManager? = nil
    var activityManager: CMMotionActivityManager? = nil
    
    var desiredAccuracy: CLLocationAccuracy = kCLLocationAccuracyBest

    var sessionToken: GMSAutocompleteSessionToken? = nil

    @objc static func requiresMainQueueSetup() -> Bool {
        return false
    }

    func initializeLocationManager() -> CLLocationManager {
        let manager = locationManager ?? CLLocationManager()
        manager.delegate = self
        manager.requestAlwaysAuthorization()
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.desiredAccuracy = desiredAccuracy
        manager.activityType = .other
        return manager
    }

    func initializeLocationManagerForGeofencing() -> CLLocationManager {
        let manager = locationManagerForGeofencing ?? CLLocationManager()
        manager.delegate = self
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.activityType = .other
        return manager
    }

    func initializeActivityManager() -> CMMotionActivityManager {
        let manager = activityManager ?? CMMotionActivityManager()
        return manager
    }

    @objc(setOptions:)
    func setOptions(options: [String: AnyObject]) -> Void {
        let iosOptions = options["ios"] as? [String: AnyObject]
        
        let locationOptions = iosOptions?["location"] as? [String: AnyObject]
        if let newLocationPriority = locationOptions?["priority"] as? Double {
            locationManager?.desiredAccuracy = newLocationPriority
        }
    }

    @objc(startGpsService:withRejecter:)
    func startGpsService(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        let background = DispatchQueue.main
        background.sync {
            locationManager = initializeLocationManager();
        }
        
        if #available(iOS 11.0, *) {
            MyEventEmitter.shared?.watchActivityPermissions(status: CMMotionActivityManager.authorizationStatus().rawValue)
        } else {
            MyEventEmitter.shared?.watchActivityPermissions(status: 3) // CMAuthorizationStatus.authorized.rawValue
        }
        
        resolve(true)
    }

    @objc(stopGpsService:withRejecter:)
    func stopGpsService(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        locationManager?.stopUpdatingLocation()
        locationManagerForGeofencing?.monitoredRegions.forEach { region in
            locationManagerForGeofencing?.stopMonitoring(for: region)
        }
        activityManager?.stopActivityUpdates()
        locationManager = nil
        locationManagerForGeofencing = nil
        activityManager = nil
        resolve(true)
    }

    @objc(startLocationUpdates)
    func startLocationUpdates() -> Void {
        let background = DispatchQueue.main
        background.sync {
            locationManager?.startUpdatingLocation()
        }
    }

    @objc(startGeofenceUpdates)
    func startGeofenceUpdates() -> Void {
        let background = DispatchQueue.main
        background.sync {
            locationManagerForGeofencing = initializeLocationManagerForGeofencing();
        }
    }

    @objc(startActivityRecognitionUpdates)
    func startActivityRecognitionUpdates() -> Void {
        activityManager = initializeActivityManager();
        activityManager?.startActivityUpdates(to: OperationQueue.main) {
            (motion) in
            
            self.getMissingActivityRecognition()

            if let motion = motion {
                
                self.sendActivities(motions: [motion])
            }
            
            let defaults = UserDefaults.standard
            defaults.set(Date().timeIntervalSince1970, forKey: "activityRecognitionLastUpdate")
        }
    }
    
    private func sendActivities(motions: [CMMotionActivity]) -> Void {
        let activitiesDict = motions.map { motion -> [String: Any] in
            
            var type: ActivityRecognitionType = .Unknown
            
            if (motion.cycling) {
                type = .OnBicycle
            } else if (motion.automotive) {
                type = .InVechicle
            } else if (motion.running) {
                type = .Running
            } else if (motion.stationary) {
                type = .Still
            } else if (motion.walking) {
                type = .Walking
            }

            let activityDict: [String: Any] = [
                "type": type.rawValue,
                "confidence": motion.confidence == CMMotionActivityConfidence.low ? 25 : motion.confidence == CMMotionActivityConfidence.medium ? 50 : 75, // TODO: to refactor
            ]

            return activityDict
        }
        
        MyEventEmitter.shared?.activitiesReceived(activities: activitiesDict)
    }
    
    private func getMissingActivityRecognition() -> Void {
        
        let defaults = UserDefaults.standard
        let activityRecognitionLastUpdate = defaults.double(forKey: "activityRecognitionLastUpdate")
        
        if (activityRecognitionLastUpdate == 0) {
            return
        }
        
        let from = Date(timeIntervalSince1970: activityRecognitionLastUpdate)
        let to = Date()
        
        activityManager?.queryActivityStarting(from: from, to: to, to: OperationQueue.main, withHandler: { motions, error in
            if let motions = motions {
                self.sendActivities(motions: motions)
            }
        })
        
        defaults.set(Date().timeIntervalSince1970, forKey: "activityRecognitionLastUpdate")
    }

    @objc(stopLocationUpdates)
    func stopLocationUpdates() -> Void {
        locationManager?.stopUpdatingLocation()
    }

    @objc(stopGeofenceUpdates)
    func stopGeofenceUpdates() -> Void {
        // TODO: change
        locationManagerForGeofencing?.monitoredRegions.forEach { region in
            locationManagerForGeofencing?.stopMonitoring(for: region)
        }
        locationManagerForGeofencing = nil
    }

    @objc(stopActivityRecognitionUpdates)
    func stopActivityRecognitionUpdates() -> Void {
        activityManager?.stopActivityUpdates()
        activityManager = nil
    }

    @objc(requestLocationPermissions:withRejecter:)
    func requestLocationPermissions(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        resolve(true)
        locationManager?.requestAlwaysAuthorization()
    }

    @objc(requestActivityPermissions:withRejecter:)
    func requestActivityPermissions(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
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
                "time": Int(location.timestamp.timeIntervalSince1970 * 1000),
                "isFromMockProvider": false,
            ];

            resolve(locationDict)
        } else {
            resolve(nil)
        }
    }

    @objc(addGeofences:withResolver:withRejecter:)
    func addGeofences(geofences:Array<[String: AnyObject]>, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {

        geofences.forEach { geofence in

            let id = geofence["id"] as! String
            let latitude = geofence["latitude"] as! Double
            let longitude = geofence["longitude"] as! Double
            let radius = geofence["radius"] as! Double

            let center = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)

            let region = CLCircularRegion(center: center, radius: radius, identifier: id)

            locationManagerForGeofencing?.startMonitoring(for: region)
        }

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

        var bounds: GMSCoordinateBounds? = nil

        if
            let northEastBoundsDict = options["northEastBounds"] as? [String: AnyObject],
            let southWestBoundsDict = options["southWestBounds"] as? [String: AnyObject] {

            let northEastBounds = CLLocationCoordinate2D(
                latitude: northEastBoundsDict["latitude"] as! CLLocationDegrees,
                longitude: northEastBoundsDict["longitude"] as! CLLocationDegrees
            )

            let southWestBounds = CLLocationCoordinate2D(
                latitude: southWestBoundsDict["latitude"] as! CLLocationDegrees,
                longitude: southWestBoundsDict["longitude"] as! CLLocationDegrees
            )

            bounds = GMSCoordinateBounds(coordinate: northEastBounds, coordinate: southWestBounds)
        }

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
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        MyEventEmitter.shared?.watchLocationPermissions(status: status.rawValue)
    }
    
    @available(iOS 14.0, *)
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        MyEventEmitter.shared?.watchLocationPermissions(status: status.rawValue)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        
        switch manager {
        case locationManager:
            let locationsDict = locations.map { (e) -> [String: Any?] in
                [
                    "latitude": e.coordinate.latitude,
                    "longitude": e.coordinate.longitude,
                    "speed": e.speed < 0 ? nil : Int(e.speed),
                    "accuracy": e.horizontalAccuracy < 0 ? nil : e.horizontalAccuracy,
                    "altitude": e.altitude,
                    "bearing": e.course < 0 ? nil : e.course,
                    "time": Int(e.timestamp.timeIntervalSince1970 * 1000),
                    "isFromMockProvider": false,
                ]
            }
            
            MyEventEmitter.shared?.locationsReceived(locations: locationsDict)
        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        switch manager {
        case locationManagerForGeofencing:
            if let region = region as? CLCircularRegion {
                let id = region.identifier

                let geofenceDict: [String: Any] = [
                    "ids": [id],
                    "transition": GeofenceTransition.Enter.rawValue,
                ]

                MyEventEmitter.shared?.geofenceReceived(geofence: geofenceDict)
            }
        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        switch manager {
        case locationManagerForGeofencing:
            if let region = region as? CLCircularRegion {
                let id = region.identifier

                let geofenceDict: [String: Any] = [
                    "ids": [id],
                    "transition": GeofenceTransition.Exit.rawValue,
                ]

                MyEventEmitter.shared?.geofenceReceived(geofence: geofenceDict)
            }
        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for region: CLRegion) {
        if (state == .unknown) { return }

        switch manager {
        case locationManagerForGeofencing:
            if let region = region as? CLCircularRegion {
                let id = region.identifier

                let geofenceDict: [String: Any] = [
                    "ids": [id],
                    "transition":
                        state == .inside
                        ? GeofenceTransition.Enter.rawValue
                        : GeofenceTransition.Exit.rawValue,
                ]

                MyEventEmitter.shared?.geofenceReceived(geofence: geofenceDict)
            }
        default:
            break
        }
    }
}
