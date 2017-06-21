package net.masonapps.csgvr.modeling;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Disposable;

import net.masonapps.csgvr.primitives.ConversionUtils;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.apache.commons.math3.geometry.euclidean.threed.PolyhedronsSet;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.SubPlane;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.masonapps.libgdxgooglevr.GdxVr;

/**
 * Created by Bob on 6/13/2017.
 */

public class Solid implements Disposable {
    private static final Vector3 dir = new Vector3();
    private static final Vector3 tmp = new Vector3();
    private static final Vector3 tmp2 = new Vector3();
    protected final Vector3 position = new Vector3();
    protected final Quaternion rotation = new Quaternion();
    private final Quaternion rotator = new Quaternion();
    private final Matrix4 inverse = new Matrix4();
    public double tolerance = 1e-10;
    public Material material = new Material();
    @Nullable
    protected PolyhedronsSet polyhedronsSet = null;
    @Nullable
    protected ModelInstance modelInstance = null;
    protected boolean isPolyhedronsSetUpdated = false;
    protected boolean isModelInstanceUpdated = false;
    private BoundingBox boundingBox = new BoundingBox();
    private Ray tempRay = new Ray();
    private Vector3 lastPosition = new Vector3();
    private Quaternion lastRotation = new Quaternion();

    public Solid(@Nullable PolyhedronsSet polyhedronsSet, @Nullable ModelInstance modelInstance) {
        this.polyhedronsSet = polyhedronsSet;
        this.modelInstance = modelInstance;
        if (this.modelInstance != null) {
            this.material = this.modelInstance.materials.first();
            this.modelInstance.transform.getTranslation(position);
            this.modelInstance.transform.getRotation(rotation);
        } else {
            this.material = new Material(ColorAttribute.createDiffuse(Color.SKY));
        }
    }

    protected Solid() {
        this(null, null);
    }

    @Nullable
    public PolyhedronsSet getPolyhedronsSet() {
        if (polyhedronsSet != null && isPolyhedronsSetUpdated) {
            polyhedronsSet.translate(new Vector3D(position.x - lastPosition.x, position.y - lastPosition.y, position.z - lastPosition.z));
            final Quaternion quat = new Quaternion(lastRotation).conjugate().mul(rotation).nor();
            polyhedronsSet.rotate(new Vector3D(0, 0, 0), new Rotation(quat.w, quat.x, quat.y, quat.z, false));
            lastPosition.set(position);
            lastRotation.set(rotation);
        }
        return polyhedronsSet;
    }

    @Nullable
    public ModelInstance getModelInstance() {
        if (!isModelInstanceUpdated) {
            updateTransformAndBounds();
        }
        return modelInstance;
    }

    private void updateTransformAndBounds() {
        if (modelInstance != null) {
            modelInstance.transform.set(position, rotation);
            inverse.set(modelInstance.transform).inv();
            modelInstance.calculateBoundingBox(boundingBox);
        }
    }

    public boolean castRay(Ray ray, Vector3 hitPoint) {
        if (!isModelInstanceUpdated) {
            updateTransformAndBounds();
        }
        if (polyhedronsSet != null) {
            if (Intersector.intersectRayBoundsFast(ray, boundingBox)) {
                tempRay.set(GdxVr.input.getInputRay());
                tempRay.mul(inverse);
                final Vector3D point = ConversionUtils.convertVector(tempRay.origin);
                final Vector3D point2 = ConversionUtils.convertVector(tempRay.direction).add(point);
                final SubPlane plane3D = (SubPlane) polyhedronsSet.firstIntersection(point, new Line(point, point2, polyhedronsSet.getTolerance()));
                if (plane3D != null) {
                    final com.badlogic.gdx.math.Plane plane = ConversionUtils.convertPlane((Plane) plane3D.getHyperplane());
                    return Intersector.intersectRayPlane(ray, plane, hitPoint);
                }
            }
        }
        return false;
    }

    public void setRotationX(float angle) {
        rotation.set(Vector3.X, angle);
        invalidate();
    }

    public void setRotationY(float angle) {
        rotation.set(Vector3.Y, angle);
        invalidate();
    }

    public void setRotationZ(float angle) {
        rotation.set(Vector3.Z, angle);
        invalidate();
    }

    public void rotateX(float angle) {
        rotator.set(Vector3.X, angle);
        rotation.mul(rotator);
        invalidate();
    }

    public void rotateY(float angle) {
        rotator.set(Vector3.Y, angle);
        rotation.mul(rotator);
        invalidate();
    }

    public void rotateZ(float angle) {
        rotator.set(Vector3.Z, angle);
        invalidate();
    }

    public void setRotation(float yaw, float pitch, float roll) {
        rotation.setEulerAngles(yaw, pitch, roll);
        invalidate();
    }

    public void setRotation(Vector3 dir, Vector3 up) {
        tmp.set(up).crs(dir).nor();
        tmp2.set(dir).crs(tmp).nor();
        rotation.setFromAxes(tmp.x, tmp2.x, dir.x, tmp.y, tmp2.y, dir.y, tmp.z, tmp2.z, dir.z);
        invalidate();
    }

    public void lookAt(Vector3 position, Vector3 up) {
        dir.set(position).sub(this.position).nor();
        setRotation(dir, up);
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public void setRotation(Quaternion q) {
        rotation.set(q);
        invalidate();
    }

    public void translateX(float units) {
        this.position.x += units;
        invalidate();
    }

    public float getX() {
        return this.position.x;
    }

    public void setX(float x) {
        this.position.x = x;
        invalidate();
    }

    public void translateY(float units) {
        this.position.y += units;
        invalidate();
    }

    public float getY() {
        return this.position.y;
    }

    public void setY(float y) {
        this.position.y = y;
        invalidate();
    }

    public void translateZ(float units) {
        this.position.z += units;
        invalidate();
    }

    public float getZ() {
        return this.position.z;
    }

    public void setZ(float z) {
        this.position.z = z;
        invalidate();
    }

    public void translate(float x, float y, float z) {
        this.position.add(x, y, z);
        invalidate();
    }

    public void translate(Vector3 trans) {
        this.position.add(trans);
        invalidate();
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        invalidate();
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 pos) {
        this.position.set(pos);
        invalidate();
    }

    private void invalidate() {
        isModelInstanceUpdated = false;
        isPolyhedronsSetUpdated = false;
    }

    @Override
    public void dispose() {
        if (modelInstance != null)
            modelInstance.model.dispose();
    }
}