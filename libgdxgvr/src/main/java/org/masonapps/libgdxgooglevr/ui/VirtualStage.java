package org.masonapps.libgdxgooglevr.ui;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import org.masonapps.libgdxgooglevr.input.VrInputProcessor;
import org.masonapps.libgdxgooglevr.math.PlaneUtils;


/**
 * Created by Bob on 1/6/2017.
 */

public class VirtualStage extends Stage implements VrInputProcessor {

    protected static float pixelSizeWorld = 1f / 500f;
    // call invalidate() after making changes for them to take effect
    protected final Vector3 position = new Vector3();
    protected final Quaternion rotation = new Quaternion();
    protected final Vector2 scale = new Vector2(1f, 1f);
    protected final Matrix4 transform = new Matrix4();
    private final Vector3 translation = new Vector3();
    public Rectangle bounds = new Rectangle();
    protected Plane plane = new Plane(Vector3.Z, 0);
    protected boolean updated = false;
    private boolean visible = true;
    private int mouseScreenX;
    private int mouseScreenY;
    private Actor mouseOverActor = null;
    private boolean isCursorOver = false;
    private Vector2 hitPoint2DPixels = new Vector2();
    private Vector3 hitPoint3D = new Vector3();
    private boolean touchable = true;
    private float radius;
    private Matrix4 batchTransform = new Matrix4();
    private Matrix4 inverseTransform = new Matrix4();
    private float activationMovement = 0.025f;
    private float activation = 0f;
    private float animationDuration = 0.25f;
    private Interpolation interpolation = new Interpolation.Pow(2);
    private float alpha = 1f;
    private boolean activationEnabled = false;

    public VirtualStage(Batch batch, int virtualPixelWidth, int virtualPixelHeight) {
        super(new ScreenViewport(), batch);
        setSize(virtualPixelWidth, virtualPixelHeight);
    }

    public VirtualStage(Batch batch, float width, float height, Matrix4 transform) {
        this(batch, (int) (width / pixelSizeWorld), (int) (height / pixelSizeWorld));
        if (transform != null) {
            this.transform.set(transform);
            invalidate();
        }
    }

    public static void setPixelSizeWorld(float pixelSizeWorld) {
        VirtualStage.pixelSizeWorld = pixelSizeWorld;
    }

    public static void setPixelsPerWorldUnit(float pixels) {
        VirtualStage.pixelSizeWorld = 1f / pixels;
    }

    public void setScale(float x, float y) {
        scale.set(x, y);
        invalidate();
    }

    public void setScale(Vector2 scale) {
        this.scale.set(scale);
        invalidate();
    }

    public void scaleX(float x) {
        scale.x *= x;
        invalidate();
    }

    public void scaleY(float y) {
        scale.y *= y;
        invalidate();
    }

    public void scale(float x, float y) {
        scale.scl(x, y);
        invalidate();
    }

    public float getScaleX() {
        return this.scale.x;
    }

    public void setScaleX(float x) {
        scale.x = x;
        invalidate();
    }

    public float getScaleY() {
        return this.scale.y;
    }

    public void setScaleY(float y) {
        scale.y = y;
        invalidate();
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
        final Quaternion rotator = Pools.obtain(Quaternion.class);
        rotator.set(Vector3.X, angle);
        rotation.mul(rotator);
        Pools.free(rotator);
        invalidate();
    }

    public void rotateY(float angle) {
        final Quaternion rotator = Pools.obtain(Quaternion.class);
        rotator.set(Vector3.Y, angle);
        rotation.mul(rotator);
        Pools.free(rotator);
        invalidate();
    }

    public void rotateZ(float angle) {
        final Quaternion rotator = Pools.obtain(Quaternion.class);
        rotator.set(Vector3.Z, angle);
        rotation.mul(rotator);
        Pools.free(rotator);
        invalidate();
    }

    public void setRotation(float yaw, float pitch, float roll) {
        rotation.setEulerAngles(yaw, pitch, roll);
        invalidate();
    }

    public void setRotation(Vector3 dir, Vector3 up) {
        final Vector3 tmp = Pools.obtain(Vector3.class);
        final Vector3 tmp2 = Pools.obtain(Vector3.class);
        tmp.set(up).crs(dir).nor();
        tmp2.set(dir).crs(tmp).nor();
        rotation.setFromAxes(tmp.x, tmp2.x, dir.x, tmp.y, tmp2.y, dir.y, tmp.z, tmp2.z, dir.z);
        invalidate();
        Pools.free(tmp);
        Pools.free(tmp2);
    }

    public void lookAt(Vector3 position, Vector3 up) {
        final Vector3 dir = Pools.obtain(Vector3.class);
        dir.set(position).sub(this.position).nor();
        setRotation(dir, up);
        Pools.free(dir);
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

    public void recalculateTransform() {
        final Vector3 tmp = Pools.obtain(Vector3.class);
        final Vector3 normal = tmp.set(Vector3.Z).mul(rotation).nor();
        if (activationEnabled)
            translation.set(normal).scl(activationMovement).add(position).lerp(position, interpolation.apply(1f - activation));
        else
            translation.set(position);
        bounds.set(0, 0, getViewport().getCamera().viewportWidth * pixelSizeWorld * scale.x, getViewport().getCamera().viewportHeight * pixelSizeWorld * scale.y);
        radius = (float) Math.sqrt(bounds.width * bounds.width + bounds.height * bounds.height);
        transform.idt().translate(translation).rotate(rotation).translate(-bounds.getWidth() * 0.5f, -bounds.getHeight() * 0.5f, 0).scale(pixelSizeWorld * scale.x, pixelSizeWorld * scale.y, 1f);
        inverseTransform.set(transform).inv();
        updated = true;
        Pools.free(tmp);
    }

    public void draw(Camera camera) {
        draw(camera, null);
    }

    public void draw(Camera camera, @Nullable Matrix4 parentTransform) {
        if (!visible) return;
        if (!updated) recalculateTransform();
        Batch batch = this.getBatch();
        getRoot().setTransform(false);
        batch.begin();
        batch.setProjectionMatrix(camera.combined);
        batchTransform.set(transform);
        if (parentTransform != null)
            batchTransform.mulLeft(parentTransform);
        batch.setTransformMatrix(batchTransform);
        getRoot().draw(batch, alpha);
        batch.end();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (activationEnabled) {
            if (isCursorOver && activation < 1f) {
                activation += delta / animationDuration;
                invalidate();
            } else if (!isCursorOver && activation > 0f) {
                activation -= delta / animationDuration;
                invalidate();
            }
        }
        mouseOverActor = fireEnterAndExit(mouseOverActor, mouseScreenX, mouseScreenY, -1);
    }

    @Override
    public void draw() {
        throw new UnsupportedOperationException("method not supported in " + VirtualStage.class.getSimpleName());
    }
    
    public void invalidate(){
        updated = false;
    }

    @Override
    public boolean performRayTest(Ray ray) {
        isCursorOver = false;
        if (!visible | !touchable) return false;
        if (!updated) recalculateTransform();
        final Ray tmpRay = Pools.obtain(Ray.class);
        tmpRay.set(ray).mul(inverseTransform);
        final Vector2 tmp = Pools.obtain(Vector2.class);
        final Vector2 center = Pools.obtain(Vector2.class);
        if (Intersector.intersectRayPlane(ray, plane, hitPoint3D)) {
            PlaneUtils.toSubSpace(plane, hitPoint3D, tmp);
            tmp.sub(bounds.getCenter(center));
            if (bounds.contains(tmp)) {
                hitPoint2DPixels.set(tmp).scl(1f / (pixelSizeWorld * scale.x), 1f / (pixelSizeWorld * scale.y));
                isCursorOver = true;
            }
        }
        Pools.free(tmpRay);
        Pools.free(tmp);
        Pools.free(center);
        return isCursorOver;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        final Vector2 tmpV2 = Pools.obtain(Vector2.class);

        tmpV2.set(screenX, screenY);

        InputEvent event = Pools.obtain(InputEvent.class);
        event.setType(InputEvent.Type.touchDown);
        event.setStage(this);
        event.setStageX(tmpV2.x);
        event.setStageY(tmpV2.y);
        event.setPointer(pointer);
        event.setButton(button);

        Actor target = hit(tmpV2.x, tmpV2.y, true);
        if (target == null) {
            if (getRoot().getTouchable() == Touchable.enabled) getRoot().fire(event);
        } else {
            target.fire(event);
        }

        boolean handled = event.isHandled();
        Pools.free(event);
        Pools.free(tmpV2);
        return handled;
    }

    @Override
    public Vector2 screenToStageCoordinates(Vector2 screenCoords) {
        return screenCoords;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return super.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseScreenX = screenX;
        mouseScreenY = screenY;

        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.mouseMoved);
        event.setStageX(screenX);
        event.setStageY(screenY);

        Actor target = hit(screenX, screenY, true);
        if (target == null) target = getRoot();

        target.fire(event);
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    private Actor fireEnterAndExit(Actor overLast, int screenX, int screenY, int pointer) {
        // Find the actor under the point.
        final Vector2 tmpV2 = Pools.obtain(Vector2.class);
        tmpV2.set(screenX, screenY);
        Actor over = hit(tmpV2.x, tmpV2.y, true);
        if (over == overLast) return overLast;

        // Exit overLast.
        if (overLast != null) {
            InputEvent event = Pools.obtain(InputEvent.class);
            event.setStage(this);
            event.setStageX(tmpV2.x);
            event.setStageY(tmpV2.y);
            event.setPointer(pointer);
            event.setType(InputEvent.Type.exit);
            event.setRelatedActor(over);
            overLast.fire(event);
            Pools.free(event);
        }
        // Enter over.
        if (over != null) {
            InputEvent event = Pools.obtain(InputEvent.class);
            event.setStage(this);
            event.setStageX(tmpV2.x);
            event.setStageY(tmpV2.y);
            event.setPointer(pointer);
            event.setType(InputEvent.Type.enter);
            event.setRelatedActor(overLast);
            over.fire(event);
            Pools.free(event);
        }
        Pools.free(tmpV2);
        return over;
    }

    @Override
    public void calculateScissors(Rectangle localRect, Rectangle scissorRect) {
        super.calculateScissors(localRect, scissorRect);
        scissorRect.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        scissorRect.set(0, 0, 0, 0);
    }

    public boolean isActivationEnabled() {
        return activationEnabled;
    }

    public void setActivationEnabled(boolean activationEnabled) {
        this.activationEnabled = activationEnabled;
    }

    public boolean isTouchable() {
        return touchable;
    }

    public void setTouchable(boolean touchable) {
        this.touchable = touchable;
    }

    @Override
    public Vector2 getHitPoint2D() {
        return hitPoint2DPixels;
    }

    @Override
    public Vector3 getHitPoint3D() {
        return hitPoint3D;
    }

    public Plane getPlane() {
        return plane;
    }

    @Override
    public boolean isCursorOver() {
        return isCursorOver;
    }

    public float getWidthWorld() {
        if (!updated) recalculateTransform();
        return getWidth() * pixelSizeWorld * getScaleX();
    }

    public float getHeightWorld() {
        if (!updated) recalculateTransform();
        return getHeight() * pixelSizeWorld * getScaleY();
    }

    public void setSize(int virtualPixelWidth, int virtualPixelHeight) {
        getViewport().update(virtualPixelWidth, virtualPixelHeight, false);
        bounds.set(0, 0, virtualPixelWidth * pixelSizeWorld, virtualPixelHeight * pixelSizeWorld);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setAnimationDuration(float animationDuration) {
        this.animationDuration = animationDuration;
    }

    public void setActivationMovement(float activationMovement) {
        this.activationMovement = activationMovement;
        setActivationEnabled(activationMovement != 0);
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }
}
