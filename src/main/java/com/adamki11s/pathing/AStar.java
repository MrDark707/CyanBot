package com.adamki11s.pathing;

/*
 * By @Adamki11s
 */


import me.xjcyan1de.cyanbot.world.Block;
import me.xjcyan1de.cyanbot.world.Location;
import me.xjcyan1de.cyanbot.world.World;

import java.util.*;


public class AStar {

    private final World world;
    private final int sx, sy, sz, ex, ey, ez;
    private final int range;
    private final String endUID;
    boolean checkOnce = false;
    private PathingResult result;
    private HashMap<String, Tile> open = new HashMap<String, Tile>();
    private HashMap<String, Tile> closed = new HashMap<String, Tile>();

    public AStar(World world, Location start, Location end, int range) throws InvalidPathException {
        this.world = world;
        boolean s = true, e = true;

        /*if (!(s = this.isLocationWalkable(start)) || !(e = this.isLocationWalkable(end))) {
            throw new InvalidPathException(s, e);
        }*/

        this.sx = start.getBlockX();
        this.sy = start.getBlockY();
        this.sz = start.getBlockZ();
        this.ex = end.getBlockX();
        this.ey = end.getBlockY();
        this.ez = end.getBlockZ();

        this.range = range;

        short sh = 0;
        Tile t = new Tile(sh, sh, sh, null);
        t.calculateBoth(sx, sy, sz, ex, ey, ez, true);
        this.open.put(t.getUID(), t);
        this.processAdjacentTiles(t);

        StringBuilder b = new StringBuilder();
        b.append(ex - sx).append(ey - sy).append(ez - sz);
        this.endUID = b.toString();
    }

    private void addToOpenList(Tile t, boolean modify) {
        if (open.containsKey(t.getUID())) {
            if (modify) {
                open.put(t.getUID(), t);
            }
        } else {
            open.put(t.getUID(), t);
        }
    }

    private void addToClosedList(Tile t) {
        if (!closed.containsKey(t.getUID())) {
            closed.put(t.getUID(), t);
        }
    }

    public Location getEndLocation() {
        return new Location(ex, ey, ez);
    }

    public PathingResult getPathingResult() {
        return this.result;
    }

    private int abs(int i) {
        return (i < 0 ? -i : i);
    }

    public ArrayList<Tile> iterate() {

        if (!checkOnce) {
            // invert the boolean flag
            checkOnce ^= true;
            if ((abs(sx - ex) > range) || (abs(sy - ey) > range) || (abs(sz - ez) > range)) {
                this.result = PathingResult.NO_PATH;
                return null;//jump out
            }
        }
        // while not at end
        Tile current = null;

        while (canContinue()) {

            // get lowest F cost square on open list
            current = this.getLowestFTile();

            // process tiles
            this.processAdjacentTiles(current);
        }

        if (this.result != PathingResult.SUCCESS) {
            return null;
        } else {
            // path found
            LinkedList<Tile> routeTrace = new LinkedList<Tile>();
            Tile parent;

            routeTrace.add(current);

            while ((parent = current.getParent()) != null) {
                routeTrace.add(parent);
                current = parent;
            }

            Collections.reverse(routeTrace);

            return new ArrayList<Tile>(routeTrace);
        }
    }

    private boolean canContinue() {
        // check if open list is empty, if it is no path has been found
        if (open.size() == 0) {
            this.result = PathingResult.NO_PATH;
            return false;
        } else {
            if (closed.containsKey(this.endUID)) {
                this.result = PathingResult.SUCCESS;
                return false;
            } else {
                return true;
            }
        }
    }

    private Tile getLowestFTile() {
        double f = 0;
        Tile drop = null;

        // get lowest F cost square
        for (Tile t : open.values()) {
            if (f == 0) {
                t.calculateBoth(sx, sy, sz, ex, ey, ez, true);
                f = t.getF();
                drop = t;
            } else {
                t.calculateBoth(sx, sy, sz, ex, ey, ez, true);
                double posF = t.getF();
                if (posF < f) {
                    f = posF;
                    drop = t;
                }
            }
        }

        // drop from open list and add to closed

        this.open.remove(drop.getUID());
        this.addToClosedList(drop);

        return drop;
    }

    private boolean isOnClosedList(Tile t) {
        return closed.containsKey(t.getUID());
    }

    // pass in the current tile as the parent
    private void processAdjacentTiles(Tile current) {

        // set of possible walk to locations adjacent to current tile
        HashSet<Tile> possible = new HashSet<Tile>(26);

        for (byte x = -1; x <= 1; x++) {
            for (byte y = -1; y <= 1; y++) {
                for (byte z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) {
                        continue;// don't check current square
                    }

                    Tile t = new Tile((short) (current.getX() + x), (short) (current.getY() + y), (short) (current.getZ() + z), current);

                    if (!t.isInRange(this.range)) {
                        // if block is out of bounds continue
                        continue;
                    }

                    if (x != 0 && z != 0 && (y == 0 || y == 1)) {
                        // check to stop jumping through diagonal blocks
                        Tile xOff = new Tile((short) (current.getX() + x), (short) (current.getY() + y), (current.getZ()), current), zOff = new Tile((current.getX()),
                                (short) (current.getY() + y), (short) (current.getZ() + z), current);
                        if (!this.isTileWalkable(xOff) && !this.isTileWalkable(zOff)) {
                            continue;
                        }
                    }

                    if (this.isOnClosedList(t)) {
                        // ignore tile
                        continue;
                    }

                    // only process the tile if it can be walked on
                    if (this.isTileWalkable(t)) {
                        t.calculateBoth(sx, sy, sz, ex, ey, ez, true);
                        possible.add(t);
                    }

                }
            }
        }

        for (Tile t : possible) {
            // get the reference of the object in the array
            Tile openRef = null;
            if ((openRef = this.isOnOpenList(t)) == null) {
                // not on open list, so add
                this.addToOpenList(t, false);
            } else {
                // is on open list, check if path to that square is better using
                // G cost
                if (t.getG() < openRef.getG()) {
                    // if current path is better, change parent
                    openRef.setParent(current);
                    // force updates of F, G and H values.
                    openRef.calculateBoth(sx, sy, sz, ex, ey, ez, true);
                }

            }
        }

    }

    private Tile isOnOpenList(Tile t) {
        return (open.containsKey(t.getUID()) ? open.get(t.getUID()) : null);
        /*
         * for (Tile o : open) { if (o.equals(t)) { return o; } } return null;
         */
    }

    private boolean isTileWalkable(Tile t) {
        Location l = new Location((sx + t.getX()), (sy + t.getY()), (sz + t.getZ()));
        Block b = world.getBlockAt(l);
        int i = b.getId();

        // lava, fire, wheat and ladders cannot be walked on, and of course air
        // 85, 107 and 113 stops npcs climbing fences and fence gates
        int d = 0;
        if (i != 8 && i != 9 && i != 10 && i != 11 && i != 51 && i != 59 && i != 65 && i != d && i != 85 && i != 107 && i != 113 && !canBlockBeWalkedThrough(i)) {
            // make sure the blocks above are air

            /*if (b.getRelative(0, 1, 0).getId() == 107) {
                // fench gate check, if closed continue
                Gate g = new Gate(b.getRelative(0, 1, 0).getData());
                return (g.isOpen() ? (b.getRelative(0, 2, 0).getId() == 0) : false);//TODO support this
            }*/
            return (canBlockBeWalkedThrough(b.getRelative(0, 1, 0).getId()) && canBlockBeWalkedThrough(b.getRelative(0, 2, 0).getId()));

        } else {
            return false;
        }
    }

    private boolean isLocationWalkable(Location l) {

        Block b = world.getBlockAt(l);
        int i = b.getId();
        int d = 0;
        System.out.println("The block ID was: " + i);
        if (i != 10 && i != 11 && i != 51 && i != 59 && i != 65 && i != d && (!canBlockBeWalkedThrough(i))) {
            // make sure the blocks above are air or can be walked through
            System.out.println("Location is: " + b.getX() + " " + b.getY() + " " + b.getZ());
            System.out.println("ID1: " + b.getRelative(0, 1, 0).getId() + " ID2: " + b.getRelative(0, 2, 0).getId());
            System.out.println("But here: " + canBlockBeWalkedThrough(b.getRelative(0, 1, 0).getId()) + " and this is: " + (b.getRelative(0, 2, 0).getId() == 0));
            return (canBlockBeWalkedThrough(b.getRelative(0, 1, 0).getId()) && canBlockBeWalkedThrough(b.getRelative(0, 2, 0).getId()));
        } else {
            return false;
        }
    }

    private boolean canBlockBeWalkedThrough(int id) {
        int d = 0;
        return (id == d || id == 8 || id == 9 || id == 6 || id == 50 || id == 63 || id == 30 || id == 31 || id == 32 || id == 37 || id == 38 || id == 39 || id == 40 || id == 55 || id == 66 || id == 75
                || id == 76 || id == 78);
    }

    @SuppressWarnings("serial")
    public class InvalidPathException extends Exception {

        private final boolean s, e;

        public InvalidPathException(boolean s, boolean e) {
            this.s = s;
            this.e = e;
        }

        public String getErrorReason() {
            StringBuilder sb = new StringBuilder();
            if (!s) {
                sb.append("Start Location was air. ");
            }
            if (!e) {
                sb.append("End Location was air.");
            }
            return sb.toString();
        }

        public boolean isStartNotSolid() {
            return (!s);
        }

        public boolean isEndNotSolid() {
            return (!e);
        }
    }

}
