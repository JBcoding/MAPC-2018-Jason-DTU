package data;

import massim.scenario.city.data.Location;

public class CExploredMap {
    public boolean explored[][];
    public double latPerSquare;
    public double lonPerSquare;

    public final int SQUARE_WIDTH = 50;

    public CCityMap cityMap;

    public CExploredMap(CCityMap ccm) {
        explored = new boolean[SQUARE_WIDTH][SQUARE_WIDTH];
        latPerSquare = (ccm.getMaxLat() - ccm.getMinLat()) / SQUARE_WIDTH;
        lonPerSquare = (ccm.getMaxLon() - ccm.getMinLon()) / SQUARE_WIDTH;

        this.cityMap = ccm;
    }

    private Location squarePosToLocation(int lat, int lon) {
        return new Location(lon * lonPerSquare + cityMap.getMinLon(), lat * latPerSquare + cityMap.getMinLat());
    }

    public void updateExplored(Location center, int vision, String agentName) {
        if (agentName.equals("agent1")) {
            //System.out.println(this);
        }
        int lonSquare = (int) ((center.getLon() - cityMap.getMinLon()) / lonPerSquare);
        int latSquare = (int) ((center.getLat() - cityMap.getMinLat()) / latPerSquare);
        latSquare = Math.min(Math.max(latSquare, 0), SQUARE_WIDTH - 1);
        lonSquare = Math.min(Math.max(lonSquare, 0), SQUARE_WIDTH - 1);

        explored[latSquare][lonSquare] = true;
        int range = 1;
        boolean change = true;
        while (change) {
            change = false;
            for (int latt = latSquare - range; latt <= latSquare + range ; latt++) {
                change |= setExplored(latt, lonSquare + range, center, vision);
                change |= setExplored(latt, lonSquare - range, center, vision);
            }
            for (int lonn = lonSquare - range; lonn <= lonSquare + range ; lonn++) {
                change |= setExplored(latSquare + range, lonn, center, vision);
                change |= setExplored(latSquare - range, lonn, center, vision);
            }
            range ++;
        }
    }

    public Location getClosestUnxploredLocation(Location center) {
        int lonSquare = (int) ((center.getLon() - cityMap.getMinLon()) / lonPerSquare);
        int latSquare = (int) ((center.getLat() - cityMap.getMinLat()) / latPerSquare);
        if (latSquare >= SQUARE_WIDTH) {
            latSquare --;
        }
        if (lonSquare >= SQUARE_WIDTH) {
            lonSquare --;
        }
        int range = 1;
        Location l = null;
        while (true) {
            for (int latt = latSquare - range; latt <= latSquare + range ; latt++) {
                l = isLocationGood(latt, lonSquare + range); if (l != null) return l;
                l = isLocationGood(latt, lonSquare - range); if (l != null) return l;
            }
            for (int lonn = lonSquare - range; lonn <= lonSquare + range ; lonn++) {
                l = isLocationGood(latSquare + range, lonn); if (l != null) return l;
                l = isLocationGood(latSquare - range, lonn); if (l != null) return l;
            }
            range ++;

            if (range > 1000) {
                resetMap();
                range = 1;
            }
        }
    }

    private void resetMap() {
        for (int lat = 0; lat < explored.length; lat++) {
            for (int lon = 0; lon < explored[lat].length; lon++) {
                explored[lat][lon] = false;
            }
        }
    }

    private Location isLocationGood(int lat, int lon) {
        if (lat >= 1 && lon >= 1 && lat < SQUARE_WIDTH && lon < SQUARE_WIDTH) {
            if (!explored[lat][lon]) {
                //System.out.println(lat + "    " + lon);
                return squarePosToLocation(lat, lon);
            }
        }
        return null;
    }

    private boolean setExplored(int lat, int lon, Location center, int vision) {
        if (lat >= 0 && lon >= 0 && lat < SQUARE_WIDTH && lon < SQUARE_WIDTH) {
            Location l = squarePosToLocation(lat, lon);
            if (cityMap.getLength(l, center) < vision) {
                explored[lat][lon] = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < SQUARE_WIDTH; i++) {
            for (int j = 0; j < SQUARE_WIDTH; j++) {
                s.append(explored[SQUARE_WIDTH - i - 1][j] ? "1" : "0");
            }
            s.append("\n");
        }
        return s.toString();
    }
}
