/**
 * @author Moritz Beller
 */
package game.airportcontrol.moveables;

import game.airportcontrol.landing.LandingDevice;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;

import org.newdawn.slick.Image;
import org.newdawn.slick.particles.ParticleIO;
import org.newdawn.slick.particles.ParticleSystem;

/**
 * @author Moritz Beller
 * 
 */
public abstract class AircraftBase {
	protected Image image;
	private double transparency;

	private Point position;
	private double curAngle;
	private ArrayList<Point> wayPoints;
	private double speed;
	private enum turningDirections {
		LEFT_TURN,
		RIGHT_TURN,
		STRAIGHT
	};
	private turningDirections turningDirection;

	private LandingDevice initiateLanding;
	private int landingPrecision;

	private final int mapScaling = 20;

	public ParticleSystem system;
	protected int mode = ParticleSystem.BLEND_COMBINE;

	public AircraftBase(Point position, int angle, double speed) {
		this.position = position;
		this.transparency = 1;
		this.curAngle = angle;
		this.wayPoints = null;
		this.initiateLanding = null;
		this.landingPrecision = 10;
		this.speed = Math.max(speed, 3.0);
		this.turningDirection = turningDirections.STRAIGHT;
	}

	public void setWayPoints(ArrayList<Point> wp) {
		this.wayPoints = wp;
	}

	public ArrayList<Point> getWayPoints() {
		return wayPoints;
	}

	public LandingDevice getInitiateLanding() {
		return initiateLanding;
	}

	public void setInitiateLanding(LandingDevice initiateLanding) {
		this.initiateLanding = initiateLanding;
		if (null != initiateLanding) {
			setLandingPrecision(initiateLanding.getLandingPrecision());
		}
		else {
			setLandingPrecision(1);
			setTransparency(1);
		}
	}

	public boolean alignAircraftToPoint(ArrayList<Point> wp) {
		if (wayPoints == null || wayPoints.size() < 1) {
			return false;
		}

		Point p = wp.get(0);
		int dy = (p.y * mapScaling) - position.y;
		int dx = (p.x * mapScaling) - position.x;

		double a;
		if (dx == 0) {
			a = 0;
		}
		else {
			a = (Math.atan((double) dy / (double) dx)) / (2 * Math.PI) * 360;
		}
		// avoid negative angles and map to angle [0;360]
		if (dx < 0) {
			a += 180;
		}
		else if(dy < 0) {
			a += 360;
		}
		
		/*if(this.turningDirection == turningDirections.STRAIGHT) {
			// no turn has been initiated on this waypoint before, i.e. aircraft is flying in a straight line atm
			// decide which is closest: a right-turn or a left-turn
			if()
		}*/
		
		if (this.turningDirection != turningDirections.STRAIGHT) {
			// TODO MMB add code for smooth curving here!
			// find out nearest angle to move to!
			if (Math.abs(Math.abs(curAngle) - Math.abs(a)) > 5) {
				if (a > curAngle)
					a = curAngle + 2;
				else
					a = curAngle - 2;
			}
			else {
			 this.turningDirection = turningDirections.STRAIGHT;
			}
		}
		
		setAngle(a);
		return true;
	}

	public void update(int width, int height, int delta) {
		alignAircraftToPoint(wayPoints);
		double ddx, ddy;
		ddx = Math.cos(((curAngle) / 360) * 2 * Math.PI) * speed;
		ddy = Math.sin(((curAngle) / 360) * 2 * Math.PI) * speed;

		int idx, idy;

		idx = (int) ddx;
		idy = (int) ddy;

		int x, y;

		x = position.x + idx;
		y = position.y + idy;

		Point targetPosition = new Point(x, y);

		if (wayPoints != null && wayPoints.size() > 0) { // not nice
			if (targetPosition.distance(new Point(wayPoints.get(0).x
					* mapScaling, wayPoints.get(0).y * mapScaling)) < 10 * mapScaling) {
				wayPoints.remove(0);
				this.turningDirection = turningDirections.STRAIGHT;
			}
		}

		// SIDE-BOUNCE_BACK
		if (x > width * mapScaling) {
			curAngle = 180 + curAngle; // 180
		}
		else if (y > height * mapScaling) {
			curAngle = 180 + curAngle; // 270
		}
		else if (x < 0) {
			curAngle = 180 + curAngle; // 0
		}
		else if (y < 0) {
			curAngle = 180 + curAngle;// 90
		}
		curAngle = curAngle % 360;
		system.update(delta); // particle effect
		setPosition(targetPosition);
	}

	public double getAngle() {
		return curAngle;
	}

	public void setAngle(double angle) {
		this.curAngle = angle;
	}

	public Image getImage() {
		return image;
	}

	public Point getPosition() {
		Point p = new Point((position.x / mapScaling),
				(position.y / mapScaling));
		return p;
	}

	public void setPosition(Point position) {
		this.position = position;
	}

	public int getLandingPrecision() {
		return landingPrecision;
	}

	public void setLandingPrecision(int landingPrecision) {
		this.landingPrecision = landingPrecision;
	}

	public double getTransparency() {
		return transparency;
	}

	public void setTransparency(double transparency) {
		this.transparency = transparency;
	}

	public void drawCollision() {
		for (int i = 2; i < 6; i++) {
			system.getEmitter(i).setEnabled(true);
		}
	}

}
