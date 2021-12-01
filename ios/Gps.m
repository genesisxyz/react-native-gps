#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(Gps, NSObject)

RCT_EXTERN_METHOD(setOptions:(NSDictionary *)options)

RCT_EXTERN_METHOD(startGpsService
                 :(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(stopGpsService
                 :(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(startLocationUpdates)

RCT_EXTERN_METHOD(startGeofenceUpdates)

RCT_EXTERN_METHOD(startActivityRecognitionUpdates)

RCT_EXTERN_METHOD(stopLocationUpdates)

RCT_EXTERN_METHOD(stopGeofenceUpdates)

RCT_EXTERN_METHOD(stopActivityRecognitionUpdates)

RCT_EXTERN_METHOD(requestLocationPermissions
                 :(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(requestActivityPermissions
                 :(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(lastLocation
                 :(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(addGeofences:(NSArray<NSDictionary *> *)geofences
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(removeGeofences:(NSArray<NSString *> *)geofencesIds
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

@end
