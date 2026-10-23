import { describe, it, expect } from 'vitest';
import { calculateHaversineDistance, EARTH_RADIUS_METERS } from '../src/lib/haversine';

describe('Haversine Distance Unit Tests', () => {
  it('should return 0 meters for identical coordinates', () => {
    expect(calculateHaversineDistance(0, 0, 0, 0)).toBe(0);
    expect(calculateHaversineDistance(-6.2088, 106.8456, -6.2088, 106.8456)).toBe(0);
    expect(calculateHaversineDistance(89.999, -179.999, 89.999, -179.999)).toBe(0);
  });

  it('should calculate distance for known coordinates (Monas to Masjid Istiqlal) with 1m tolerance', () => {
    // Monas: -6.175392, 106.827153
    // Istiqlal: -6.170170, 106.831713
    // Haversine with R=6,371,000 m is 768.96 meters
    const dist = calculateHaversineDistance(-6.175392, 106.827153, -6.170170, 106.831713);
    expect(Math.abs(dist - 768.96)).toBeLessThanOrEqual(0.1);
  });

  it('should calculate distance for inter-city pair (Paris Eiffel Tower to London Big Ben) with 1m tolerance', () => {
    // Eiffel Tower: 48.858370, 2.294481
    // Big Ben: 51.500729, -0.124625
    // Spherical Haversine distance with R=6,371,000 m is ~340,544.8 meters
    const dist = calculateHaversineDistance(48.858370, 2.294481, 51.500729, -0.124625);
    expect(Math.abs(dist - 340544.8)).toBeLessThanOrEqual(1);
  });

  it('should correctly handle crossing the Prime Meridian (0 longitude)', () => {
    // Two points across Greenwich meridian: 51.4769 N, 0.0005 E to 51.4769 N, 0.0005 W (-0.0005)
    // Distance ~ 69.45 m
    const dist = calculateHaversineDistance(51.4769, 0.0005, 51.4769, -0.0005);
    expect(dist).toBeCloseTo(69.45, 0);
  });

  it('should correctly handle crossing the Antimeridian (180 / -180 longitude)', () => {
    // Two points across antimeridian on the Equator: (0, 179.9) and (0, -179.9)
    // The shortest path across 180 meridian is 0.2 degrees
    // Exact distance = 0.2 * (pi / 180) * 6,371,000 = 22,239.35 m
    const dist = calculateHaversineDistance(0, 179.9, 0, -179.9);
    expect(Math.abs(dist - 22239.35)).toBeLessThanOrEqual(1);

    // Opposite direction
    const distRev = calculateHaversineDistance(0, -179.9, 0, 179.9);
    expect(Math.abs(distRev - 22239.35)).toBeLessThanOrEqual(1);
  });

  it('should correctly calculate distance at high latitude (Arctic / near North Pole)', () => {
    // Point 1: 89.0 N, 0.0 E
    // Point 2: 89.0 N, 180.0 E
    // The shortest path is directly across the North Pole: 2 degrees total
    // Distance = 2 * (pi / 180) * 6,371,000 = 222,389.85 m
    const dist = calculateHaversineDistance(89.0, 0.0, 89.0, 180.0);
    expect(Math.abs(dist - 222389.85)).toBeLessThanOrEqual(1);
  });

  it('should correctly calculate distance at high latitude (Antarctica / near South Pole)', () => {
    // Point 1: -89.0 S, 0.0 E
    // Point 2: -89.0 S, 180.0 E
    // Shortest path over South Pole = 2 degrees total
    const dist = calculateHaversineDistance(-89.0, 0.0, -89.0, 180.0);
    expect(Math.abs(dist - 222389.85)).toBeLessThanOrEqual(1);
  });

  it('should be symmetric: dist(A, B) === dist(B, A)', () => {
    const d1 = calculateHaversineDistance(-6.123, 106.123, 12.345, -70.123);
    const d2 = calculateHaversineDistance(12.345, -70.123, -6.123, 106.123);
    expect(d1).toBe(d2);
  });
});
