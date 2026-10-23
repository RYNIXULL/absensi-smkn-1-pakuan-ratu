/**
 * Earth radius in meters according to standard WGS84 / IUGG volumetric mean radius.
 */
export const EARTH_RADIUS_METERS = 6371000;

/**
 * Calculates the great-circle distance between two points on Earth using the Haversine formula.
 *
 * @param lat1 Latitude of first point in degrees (-90 to 90)
 * @param lon1 Longitude of first point in degrees (-180 to 180)
 * @param lat2 Latitude of second point in degrees (-90 to 90)
 * @param lon2 Longitude of second point in degrees (-180 to 180)
 * @returns Distance in meters
 */
export function calculateHaversineDistance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  // If points are identical, return exactly 0
  if (lat1 === lat2 && lon1 === lon2) {
    return 0;
  }

  const toRad = (degrees: number) => (degrees * Math.PI) / 180;

  const phi1 = toRad(lat1);
  const phi2 = toRad(lat2);
  const deltaPhi = toRad(lat2 - lat1);

  // Normalize delta lambda to [-PI, PI] to handle antimeridian crossing cleanly
  let diffLon = lon2 - lon1;
  while (diffLon > 180) diffLon -= 360;
  while (diffLon < -180) diffLon += 360;
  const deltaLambda = toRad(diffLon);

  const sinHalfDeltaPhi = Math.sin(deltaPhi / 2);
  const sinHalfDeltaLambda = Math.sin(deltaLambda / 2);

  const a =
    sinHalfDeltaPhi * sinHalfDeltaPhi +
    Math.cos(phi1) * Math.cos(phi2) * sinHalfDeltaLambda * sinHalfDeltaLambda;

  // Clamp a to [0, 1] to avoid NaN due to floating-point imprecision
  const clampedA = Math.min(1, Math.max(0, a));
  const c = 2 * Math.atan2(Math.sqrt(clampedA), Math.sqrt(1 - clampedA));

  return EARTH_RADIUS_METERS * c;
}
