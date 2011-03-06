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
 *         The aircraft angle is in [0;360] always It should be noted that the
 *         nose of the aircraft when pointing to the right hand side denotes the
 *         0, angle. A clockwise rotation from this direction onward increases
 *         the angle
 * 
 *         All angles and distances are relative to middle of aircraft which is
 *         defined to be centre of picture.
 */
public abstract class AircraftBase {
	protected Image image;
	private double transparency;

	private Point position;
	private double curAngle;
	private ArrayList<Point> wayPoints;
	private double speed;

	private enum turningDirections {
		LEFT_TURN, RIGHT_TURN, STRAIGHT
	};

	private turningDirections turningDirection;

	private double turningSpeed;
	private double requiredDistanceToWaypoint;

	private LandingDevice initiateLanding;
	private int landingPrecision;

	private final int mapScaling = 20;

	public ParticleSystem system;
	protected int mode = ParticleSystem.BLEND_COMBINE;

	public AircraftBase(Point position, int angle, double speed,
			double turningSpeed, double requiredDistanceToWaypoint) {
		this.position = position;
		this.transparency = 1;
		this.curAngle = angle;
		this.wayPoints = null;
		this.initiateLanding = null;
		this.landingPrecision = 10;
		this.speed = Math.max(speed, 3.0);
		this.turningSpeed = turningSpeed;
		this.requiredDistanceToWaypoint = requiredDistanceToWaypoint;
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
		} else {
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
		if (dx == 0) { // division through 0 undefined
			a = 0;
		} else { // calculate new heading through arcus tangens. Note: arctan in
					// [-pi/2;pi/2], map to coordinates [-90;+90]
			a = (Math.atan((double) dy / (double) dx)) / (2 * Math.PI) * 360;
		}
		// avoid negative angles and map to full range angle [0;360]
		if (dx < 0) {
			a += 180;
		} else if (dy < 0) {
			a += 360;
		}

		if (this.turningDirection == turningDirections.STRAIGHT) {
			// no turn has been initiated on this waypoint before, i.e. aircraft
			// is flying in a straight line atm
			if (Math.abs(a - curAngle) > this.turningSpeed) {
				// decide which is closest: a right-turn or a left-turn
				if ((a > curAngle && a < curAngle + 180)
						|| (a < (curAngle + 180) % 360 && curAngle >= 180))
					this.turningDirection = turningDirections.RIGHT_TURN;
				else
					this.turningDirection = turningDirections.LEFT_TURN;
			}
		}

		if (this.turningDirection != turningDirections.STRAIGHT) {
			// perform the actual smooth curving
			if (this.turningDirection == turningDirections.LEFT_TURN) {
				curAngle = (curAngle - this.turningSpeed) % 360;
				if (curAngle < 0)
					curAngle += 360;
			} else
				curAngle = (curAngle + this.turningSpeed) % 360;

		}

		if (Math.abs(a - curAngle) <= this.turningSpeed) {
			setAngle(a);
			this.turningDirection = turningDirections.STRAIGHT;
		} else {
			setAngle(curAngle);
		}
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
					* mapScaling, wayPoints.get(0).y * mapScaling)) < requiredDistanceToWaypoint
					* mapScaling) {
				wayPoints.remove(0);
				this.turningDirection = turningDirections.STRAIGHT;
			}
		}

		// wall bounce back
		if (x > width * mapScaling) {
			curAngle = 180 + curAngle; // 180
		} else if (y > height * mapScaling) {
			curAngle = 180 + curAngle; // 270
		} else if (x < 0) {
			curAngle = 180 + curAngle; // 0
		} else if (y < 0) {
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
	
	public double getRequiredDistanceToWaypoint() {
		return requiredDistanceToWaypoint;
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
