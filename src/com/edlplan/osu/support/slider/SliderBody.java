package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.osudroid.math.MathUtils;
import com.osudroid.math.Vector2;
import com.osudroid.math.LinePath;
import com.osudroid.ui.v2.game.UITriangleMesh;
import com.reco1l.verktex.data.Color4;
import com.reco1l.verktex.time.FrameClock;
import com.reco1l.verktex.ui.container.UIContainer;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class SliderBody extends UIContainer {

    private RenderPathCache cache;
    private static final float lengthUpdateEpsilon = 0.001f;
    private static final float pointMergeDistance = 0.01f;

    private float maxPathLength;

    private final UITriangleMesh background;

    private final UITriangleMesh border;

    private final UITriangleMesh hint;

    private float backgroundWidth;

    private float borderWidth;

    private float hintWidth;

    private float startLength = 0;

    private float endLength = 0;

    private boolean shouldRebuildVertices = true;

    // Reused temporary vectors for the end-only hybrid fast path.
    private final Vector2 tmpOthExpand = new Vector2(0f);
    private final Vector2 tmpStartL = new Vector2(0f);
    private final Vector2 tmpStartR = new Vector2(0f);
    private final Vector2 tmpEndL = new Vector2(0f);
    private final Vector2 tmpEndR = new Vector2(0f);
    private final Vector2 tmpCapCurrent = new Vector2(0f);
    private final Vector2 tmpCapCurrent2 = new Vector2(0f);
    private final Vector2 tmpCutPoint = new Vector2(0f);

    // Tracks monotonic boundary movement during snaking to reduce segment lookups.
    private int lastFastEndSegmentIndex = -1;
    private int lastFastStartSegmentIndex = -1;
    private float lastFastEndLength = Float.NaN;
    private float lastFastStartLength = Float.NaN;

    public SliderBody(boolean allowHint) {

        if (allowHint) {
            hint = new UITriangleMesh();
            hint.setVisible(false);
            hint.setClearDepthBeforeDraw(true);
            hint.setDepthState(UITriangleMesh.DEPTH_LESS);
        } else {
            hint = null;
        }

        border = new UITriangleMesh();
        border.setDepthState(UITriangleMesh.DEPTH_DEFAULT);
        add(0, border);

        background = new UITriangleMesh();
        background.setDepthState(UITriangleMesh.DEPTH_DEFAULT);
        add(0, background);

        if (hint != null) {
            add(0, hint);
        }

        setHintVisible(false);
    }

    public void init(boolean beginEmpty, Vector2 position, RenderPathCache cache) {
        this.cache = cache;

        clearLayer(hint);
        clearLayer(background);
        clearLayer(border);

        if (cache.sourcePath == null || cache.pointCount == 0 || cache.sourcePath.size() == 0) {
            maxPathLength = 0;
            startLength = 0;
            endLength = 0;
            shouldRebuildVertices = false;
            lastFastEndSegmentIndex = -1;
            lastFastStartSegmentIndex = -1;
            lastFastEndLength = Float.NaN;
            lastFastStartLength = Float.NaN;
            setPosition(position.x, position.y);
            return;
        }

        maxPathLength = cache.sourcePath.getLength();
        startLength = 0;

        if (beginEmpty) {
            endLength = 0;
        } else {
            endLength = maxPathLength;
        }

        shouldRebuildVertices = true;
        lastFastEndSegmentIndex = -1;
        lastFastStartSegmentIndex = -1;
        lastFastEndLength = Float.NaN;
        lastFastStartLength = Float.NaN;
        setPosition(position.x, position.y);
    }


    public void setBackgroundWidth(float value) {
        backgroundWidth = value;
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        background.setColor(new Color4(r, g, b, a));
    }

    public void setBackgroundColor(Color4 color, float alpha) {
        background.setColor(color);
        background.setAlpha(alpha);
    }


    public void setHintVisible(boolean visible) {
        if (hint != null) {
            hint.setVisible(visible);
            background.setClearDepthBeforeDraw(!visible);
            background.setDepthState(visible ? UITriangleMesh.DEPTH_DEFAULT : UITriangleMesh.DEPTH_LESS);
        } else {
            background.setClearDepthBeforeDraw(true);
            background.setDepthState(UITriangleMesh.DEPTH_LESS);
        }
    }

    public void setHintWidth(float value) {
        hintWidth = value;
    }

    public void setHintColor(float r, float g, float b, float a) {
        if (hint != null) {
            hint.setColor(new Color4(r, g, b, a));
        }
    }

    public void setHintColor(Color4 color, float alpha) {
        if (hint != null) {
            hint.setColor(color);
            hint.setAlpha(alpha);
        }
    }


    public void setTrackBorderWidth(float value) {
        borderWidth = value;
    }

    public void setTrackBorderColor(Color4 color) {
        border.setColor(color);
    }

    private LinePath fitToCachedPath(LinePath source, LinePath cache) {
        cache.clear();

        int sourceSize = source.size();
        Vector2 previousPoint = null;

        for (int i = 0; i < sourceSize; i++) {
            Vector2 currentPoint = source.get(i);

            if (previousPoint == null || i == sourceSize - 1 || LinePath.distance(previousPoint, currentPoint) > pointMergeDistance) {
                cache.add(currentPoint);
                previousPoint = currentPoint;
            }
        }

        return cache;
    }

    private float getClampedBackgroundWidth() {
        return Math.max(0, backgroundWidth);
    }

    private float getPlayableWidth() {
        return getClampedBackgroundWidth() - MathUtils.clamp(borderWidth, 0, getClampedBackgroundWidth());
    }

    private float getClampedHintWidth() {
        return MathUtils.clamp(hintWidth, 0, getPlayableWidth());
    }

    private void buildVertices(LinePath subPath) {

        TriangleBuilder builder = cache.triangleBuilder;

        if (hint != null && hint.isVisible()) {
            cache.drawLinePath
                    .reset(subPath, getClampedHintWidth())
                    .computeTriangles(builder)
                    .applyVertices(hint.getVertices());
        }

        cache.drawLinePath
                .reset(subPath, getPlayableWidth())
                .computeTriangles(builder)
                .applyVertices(background.getVertices());

        cache.drawLinePath
                .reset(subPath, getClampedBackgroundWidth())
                .computeTriangles(builder)
                .applyVertices(border.getVertices());
    }


    @Override
    protected void onUpdate(@NotNull FrameClock clock) {

        if (cache != null && shouldRebuildVertices) {
            shouldRebuildVertices = false;

            float clampedStartLength = MathUtils.clamp(startLength, 0, maxPathLength);
            float clampedEndLength = MathUtils.clamp(endLength, clampedStartLength, maxPathLength);

            // Full-length path can be built directly without slicing/allocating an intermediate path.
            if (clampedStartLength <= lengthUpdateEpsilon &&
                Math.abs(clampedEndLength - maxPathLength) <= lengthUpdateEpsilon) {
                buildVertices(cache.sourcePath);
            } else {
                // Hybrid reuse path is only valid when one edge is anchored to an endpoint.
                // Otherwise, fallback to exact cutPath triangulation.
                boolean isBuiltByFastPath =
                    (clampedStartLength <= lengthUpdateEpsilon && tryBuildFastEndLength(clampedEndLength)) ||
                    (Math.abs(clampedEndLength - maxPathLength) <= lengthUpdateEpsilon && tryBuildFastStartLength(clampedStartLength));

                if (!isBuiltByFastPath) {
                    var cutPath = cache.sourcePath.cut(clampedStartLength, clampedEndLength);
                    buildVertices(fitToCachedPath(cutPath, cache.path));
                }
            }
        }

        super.onUpdate(clock);
    }

    @Override
    public boolean intersectsSceneRect(float x, float y, float width, float height) {
        // The path can extend in any direction from the position, so the bounds say nothing about what's visible.
        return true;
    }

    public void setStartLength(float length) {
        float clampedLength = Math.max(0, Math.min(length, maxPathLength));

        if (Math.abs(clampedLength - startLength) <= lengthUpdateEpsilon) {
            return;
        }

        startLength = clampedLength;
        shouldRebuildVertices = true;
    }

    public void setEndLength(float length) {
        float clampedLength = MathUtils.clamp(length, 0, maxPathLength);

        if (Math.abs(clampedLength - endLength) <= lengthUpdateEpsilon) {
            return;
        }

        endLength = clampedLength;
        shouldRebuildVertices = true;
    }


    public static RenderPathCache createCache(@NotNull LinePath renderPath, float backgroundWidth, float borderWidth,
                                              boolean isHintVisible, float hintWidth, boolean computeFullCache) {
        RenderPathCache cache = new RenderPathCache();

        cache.sourcePath = renderPath;

        if (renderPath.size() == 0) {
            cache.pointCount = 0;
            cache.segmentCount = 0;
            return cache;
        }

        float clampedBackgroundWidth = Math.max(0, backgroundWidth);
        float clampedBorderWidth = MathUtils.clamp(borderWidth, 0, clampedBackgroundWidth);
        float playableWidth = clampedBackgroundWidth - clampedBorderWidth;
        float clampedHintWidth = MathUtils.clamp(hintWidth, 0, playableWidth);

        buildPointLengthCache(renderPath, cache);
        buildSegmentThetaCache(renderPath, cache);

        cache.segmentCount = Math.max(0, cache.pointCount - 1);

        if (computeFullCache) {
            TriangleBuilder builder = cache.triangleBuilder;

            // Build once to capture per-segment quad offsets used by hybrid prefix/suffix reuse.
            float offsetProbeWidth = playableWidth > 0 ? playableWidth : clampedBackgroundWidth;

            if (offsetProbeWidth > 0) {
                cache.drawLinePath.reset(renderPath, offsetProbeWidth).computeTriangles(builder);
                cache.segmentCount = cache.drawLinePath.getSegmentQuadStartCount();

                if (cache.segmentQuadStartOffsets.length < cache.segmentCount) {
                    cache.segmentQuadStartOffsets = new int[cache.segmentCount];
                }

                for (int i = 0; i < cache.segmentCount; i++) {
                    cache.segmentQuadStartOffsets[i] = cache.drawLinePath.getSegmentQuadStartOffset(i);
                }
            }

            if (isHintVisible && clampedHintWidth > 0) {
                cache.drawLinePath.reset(renderPath, clampedHintWidth).computeTriangles(builder);
                cache.hint.setCache(builder);
            }

            if (playableWidth > 0) {
                cache.drawLinePath.reset(renderPath, playableWidth).computeTriangles(builder);
                cache.background.setCache(builder);
            }

            if (clampedBackgroundWidth > 0) {
                cache.drawLinePath.reset(renderPath, clampedBackgroundWidth).computeTriangles(builder);
                cache.border.setCache(builder);
            }
        }

        return cache;
    }

    private static void buildPointLengthCache(LinePath path, RenderPathCache cache) {
        int size = path.size();

        if (cache.pointLengths.length < size) {
            cache.pointLengths = new float[size];
        }

        float length = 0;
        cache.pointLengths[0] = 0;

        for (int i = 1; i < size; i++) {
            length += LinePath.distance(path.get(i - 1), path.get(i));
            cache.pointLengths[i] = length;
        }

        cache.pointCount = size;
    }

    private static void buildSegmentThetaCache(LinePath path, RenderPathCache cache) {
        int segmentCount = Math.max(0, path.size() - 1);

        if (cache.segmentThetas.length < segmentCount) {
            cache.segmentThetas = new float[segmentCount];
        }

        for (int i = 0; i < segmentCount; i++) {
            cache.segmentThetas[i] = LinePath.angle(path.get(i), path.get(i + 1));
        }
    }

    private boolean tryBuildFastEndLength(float endLength) {
        // End-moving fast path: reuse full-path prefix and rebuild only the boundary segment + end cap.
        if (cache == null ||
            cache.pointCount < 2 ||
            cache.segmentCount < 1) {
            return false;
        }

        float clampedEnd = MathUtils.clamp(endLength, 0, maxPathLength);

        if (clampedEnd <= lengthUpdateEpsilon || clampedEnd >= maxPathLength - lengthUpdateEpsilon) {
            return false;
        }

        int segmentIndex = getSegmentIndexForEndLength(clampedEnd);

        if (segmentIndex < 0 || segmentIndex >= cache.segmentCount) {
            return false;
        }

        Vector2 segmentStart = cache.sourcePath.get(segmentIndex);
        Vector2 cutPoint = interpolatePointAtLength(clampedEnd, segmentIndex, tmpCutPoint);
        float segmentTheta = cache.segmentThetas[segmentIndex];

        if (hint != null && hint.isVisible()) {
            float width = getClampedHintWidth();

            if (width <= 0) {
                clearLayer(hint);
            } else if (!buildFastLayer(hint, cache.hint, width, segmentIndex, segmentStart, cutPoint,
                    segmentTheta)) {
                return false;
            }
        }

        if (!buildFastLayer(background, cache.background, getPlayableWidth(),
            segmentIndex, segmentStart, cutPoint, segmentTheta)) {
            return false;
        }

        return buildFastLayer(border, cache.border, getClampedBackgroundWidth(),
            segmentIndex, segmentStart, cutPoint, segmentTheta);
    }

    private boolean tryBuildFastStartLength(float startLength) {
        // Start-moving fast path: rebuild start boundary and append cached suffix.
        if (cache == null ||
            cache.pointCount < 2 ||
            cache.segmentCount < 1) {
            return false;
        }

        float clampedStart = MathUtils.clamp(startLength, 0, maxPathLength);

        if (clampedStart <= lengthUpdateEpsilon || clampedStart >= maxPathLength - lengthUpdateEpsilon) {
            return false;
        }

        int segmentIndex = getSegmentIndexForStartLength(clampedStart);

        if (segmentIndex < 0 || segmentIndex >= cache.segmentCount) {
            return false;
        }

        Vector2 segmentEnd = cache.sourcePath.get(segmentIndex + 1);
        Vector2 cutPoint = interpolatePointAtLength(clampedStart, segmentIndex, tmpCutPoint);
        float segmentTheta = cache.segmentThetas[segmentIndex];

        if (hint != null && hint.isVisible()) {
            float width = getClampedHintWidth();

            if (width <= 0) {
                clearLayer(hint);
            } else if (!buildFastLayerFromStart(hint, cache.hint, width, segmentIndex, segmentEnd, cutPoint,
                    segmentTheta)) {
                return false;
            }
        }

        if (!buildFastLayerFromStart(background, cache.background, getPlayableWidth(),
            segmentIndex, segmentEnd, cutPoint, segmentTheta)) {
            return false;
        }

        return buildFastLayerFromStart(border, cache.border, getClampedBackgroundWidth(),
            segmentIndex, segmentEnd, cutPoint, segmentTheta);
    }

    private boolean buildFastLayer(UITriangleMesh target,
                                   RenderPathCache.ComponentCache cache,
                                   float width,
                                   int segmentIndex,
                                   Vector2 segmentStart,
                                   Vector2 cutPoint,
                                   float segmentTheta) {
        // Prefix reuse layout: [cached triangles before boundary] + [rebuilt boundary].
        if (width <= 0 || !cache.hasCache || cache.vertexLength <= 0 || segmentIndex >= this.cache.segmentCount) {
            return false;
        }

        int prefixLength = this.cache.segmentQuadStartOffsets[segmentIndex];
        TriangleBuilder builder = this.cache.triangleBuilder;

        if (builder.ary.length < cache.vertexLength + 128) {
            builder.ary = Arrays.copyOf(builder.ary, cache.vertexLength + 128);
        }

        System.arraycopy(cache.vertices, 0, builder.ary, 0, prefixLength);
        builder.length = prefixLength;
        builder.maxX = cache.maxX;
        builder.maxY = cache.maxY;

        if (LinePath.distance(segmentStart, cutPoint) > lengthUpdateEpsilon) {
            addLineQuad(builder, segmentStart, cutPoint, width);
            addEndCap(builder, cutPoint, segmentTheta - MathUtils.HALF_PI_F, width);
        }

        builder.applyVertices(target.getVertices());
        return true;
    }

    private boolean buildFastLayerFromStart(UITriangleMesh target,
                                            RenderPathCache.ComponentCache cache,
                                            float width,
                                            int segmentIndex,
                                            Vector2 segmentEnd,
                                            Vector2 cutPoint,
                                            float segmentTheta) {
        // Suffix reuse layout: [rebuilt boundary at start] + [cached triangles after boundary].
        if (width <= 0 || !cache.hasCache || cache.vertexLength <= 0 || segmentIndex >= this.cache.segmentCount) {
            return false;
        }

        TriangleBuilder builder = this.cache.triangleBuilder;

        if (builder.ary.length < cache.vertexLength + 128) {
            builder.ary = Arrays.copyOf(builder.ary, cache.vertexLength + 128);
        }

        builder.length = 0;
        builder.maxX = cache.maxX;
        builder.maxY = cache.maxY;

        addStartCap(builder, cutPoint, segmentTheta + MathUtils.HALF_PI_F, width);

        if (LinePath.distance(cutPoint, segmentEnd) > lengthUpdateEpsilon) {
            addLineQuad(builder, cutPoint, segmentEnd, width);
        }

        if (segmentIndex < this.cache.segmentCount - 1) {
            float nextTheta = this.cache.segmentThetas[segmentIndex + 1];
            addLineCap(builder, segmentEnd, segmentTheta - MathUtils.HALF_PI_F, nextTheta - segmentTheta, width);

            int suffixOffset = this.cache.segmentQuadStartOffsets[segmentIndex + 1];
            int suffixLength = cache.vertexLength - suffixOffset;

            if (suffixLength > 0) {
                System.arraycopy(cache.vertices, suffixOffset, builder.ary, builder.length, suffixLength);
                builder.length += suffixLength;
            }
        } else {
            addEndCap(builder, segmentEnd, segmentTheta - MathUtils.HALF_PI_F, width);
        }

        builder.applyVertices(target.getVertices());
        return true;
    }

    private void clearLayer(UITriangleMesh target) {
        if (target == null) {
            return;
        }

        target.getVertices().length = 0;
    }

    private int getSegmentIndexForEndLength(float length) {
        int segmentIndex = getSegmentIndexForLength(length, false, lastFastEndSegmentIndex, lastFastEndLength);

        lastFastEndSegmentIndex = segmentIndex;
        lastFastEndLength = length;

        return segmentIndex;
    }

    private int getSegmentIndexForStartLength(float length) {
        int segmentIndex = getSegmentIndexForLength(length, true, lastFastStartSegmentIndex, lastFastStartLength);

        lastFastStartSegmentIndex = segmentIndex;
        lastFastStartLength = length;

        return segmentIndex;
    }

    private int getSegmentIndexForLength(float length, boolean preferNextOnExactPoint,
                                         int previousSegmentIndex, float previousLength) {
        // Map a measured length to the segment containing that length.
        // For start cuts at exact points, prefer the next segment to avoid reusing removed geometry.
        int segmentCount = cache.segmentCount;

        if (!Float.isNaN(previousLength) && previousSegmentIndex >= 0 && previousSegmentIndex < segmentCount) {
            int segmentIndex = previousSegmentIndex;

            if (length >= previousLength) {
                while (segmentIndex + 1 < segmentCount && cache.pointLengths[segmentIndex + 1] < length) {
                    segmentIndex++;
                }
            } else {
                while (segmentIndex > 0 && cache.pointLengths[segmentIndex] > length) {
                    segmentIndex--;
                }
            }

            // If next exact point is preferred, select it if its length falls within the epsilon.
            if (preferNextOnExactPoint && segmentIndex + 1 < cache.pointCount - 1) {
                float pointLength = cache.pointLengths[segmentIndex + 1];

                if (Math.abs(pointLength - length) <= lengthUpdateEpsilon) {
                    return segmentIndex + 1;
                }
            }

            return segmentIndex;
        }

        int left = 0;
        int right = cache.pointCount - 1;

        while (left <= right) {
            int mid = left + ((right - left) >> 1);

            if (cache.pointLengths[mid] < length) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        int pointIndex = Math.max(1, Math.min(left, cache.pointCount - 1));
        int segmentIndex = pointIndex - 1;

        if (preferNextOnExactPoint && pointIndex < cache.pointCount - 1) {
            float pointLength = cache.pointLengths[pointIndex];

            if (Math.abs(pointLength - length) <= lengthUpdateEpsilon) {
                segmentIndex = pointIndex;
            }
        }

        return Math.max(0, Math.min(segmentIndex, cache.segmentCount - 1));
    }

    private Vector2 interpolatePointAtLength(float length, int segmentIndex, Vector2 out) {
        float startLength = cache.pointLengths[segmentIndex];
        float endLength = cache.pointLengths[segmentIndex + 1];
        float segmentLength = endLength - startLength;

        Vector2 startPoint = cache.sourcePath.get(segmentIndex);

        if (segmentLength <= lengthUpdateEpsilon) {
            out.set(startPoint);
            return out;
        }

        float t = MathUtils.clamp((length - startLength) / segmentLength, 0, 1);
        Vector2 endPoint = cache.sourcePath.get(segmentIndex + 1);

        out.set(
            startPoint.x + (endPoint.x - startPoint.x) * t,
            startPoint.y + (endPoint.y - startPoint.y) * t
        );

        return out;
    }

    private void addLineQuad(TriangleBuilder builder, Vector2 pointStart, Vector2 pointEnd, float width) {
        tmpOthExpand.set(LinePath.perpendicular(pointStart, pointEnd, tmpOthExpand));
        tmpOthExpand.scale(width);

        tmpStartL.set(pointStart);
        tmpStartL.add(tmpOthExpand);

        tmpStartR.set(pointStart);
        tmpStartR.subtract(tmpOthExpand);

        tmpEndL.set(pointEnd);
        tmpEndL.add(tmpOthExpand);

        tmpEndR.set(pointEnd);
        tmpEndR.subtract(tmpOthExpand);

        builder.add(pointStart, pointEnd, tmpEndL);
        builder.add(pointStart, tmpEndL, tmpStartL);
        builder.add(pointStart, tmpEndR, pointEnd);
        builder.add(pointStart, tmpStartR, tmpEndR);
    }

    private void addEndCap(TriangleBuilder builder, Vector2 origin, float theta, float width) {
        addLineCap(builder, origin, theta, MathUtils.PI_F, width);
    }

    private void addStartCap(TriangleBuilder builder, Vector2 origin, float theta, float width) {
        addLineCap(builder, origin, theta, MathUtils.PI_F, width);
    }

    private void addLineCap(TriangleBuilder builder, Vector2 origin, float theta, float thetaDiff, float width) {
        final float step = MathUtils.PI_F / 24;

        float dir = Math.signum(thetaDiff);
        thetaDiff *= dir;
        int amountPoints = (int) Math.ceil(thetaDiff / step);

        if (dir < 0) {
            theta += MathUtils.PI_F;
        }

        tmpCapCurrent.set(LinePath.pointOnCircle(theta, tmpCapCurrent));
        tmpCapCurrent.scale(width);
        tmpCapCurrent.add(origin);

        for (int i = 1; i <= amountPoints; i++) {
            tmpCapCurrent2.set(LinePath.pointOnCircle(theta + dir * Math.min(i * step, thetaDiff), tmpCapCurrent2));
            tmpCapCurrent2.scale(width);
            tmpCapCurrent2.add(origin);

            builder.add(origin, tmpCapCurrent, tmpCapCurrent2);
            tmpCapCurrent.set(tmpCapCurrent2);
        }
    }

    public static class RenderPathCache {
        public final LinePath path = new LinePath();
        public final TriangleBuilder triangleBuilder = new TriangleBuilder();
        public final DrawLinePath drawLinePath = new DrawLinePath();

        public LinePath sourcePath;

        // Cumulative length at each path point, used to locate boundary segments without path slicing.
        public float[] pointLengths = new float[0];
        public int pointCount;

        // Quad start offsets for each segment in the precached triangulation output.
        public int[] segmentQuadStartOffsets = new int[0];
        public int segmentCount;

        // Per-segment theta values for the source path.
        public float[] segmentThetas = new float[0];

        public ComponentCache hint = new ComponentCache();
        public ComponentCache background = new ComponentCache();
        public ComponentCache border = new ComponentCache();

        // Full precached vertex buffer for a visual layer.
        public static class ComponentCache {
            public float[] vertices = new float[0];
            public int vertexLength;
            public float maxX;
            public float maxY;
            public boolean hasCache;

            public void setCache(TriangleBuilder builder) {
                vertices = Arrays.copyOf(builder.ary, builder.length);
                vertexLength = builder.length;
                maxX = builder.maxX;
                maxY = builder.maxY;
                hasCache = true;
            }
        }
    }
}
