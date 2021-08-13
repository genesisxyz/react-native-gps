import { NativeModules } from 'react-native';

type GpsType = {
  multiply(a: number, b: number): Promise<number>;
};

const { Gps } = NativeModules;

export default Gps as GpsType;
