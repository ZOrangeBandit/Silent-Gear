package net.silentchaos512.gear.client.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.Direction;
import com.mojang.math.Transformation;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.TRSRTransformer;

import java.util.List;

public abstract class LayeredModel<T extends IModelGeometry<T>> implements IModelGeometry<T> {
    // Quad builders (credit to Tetra, https://github.com/mickelus/tetra/blob/master/src/main/java/se/mickelus/tetra/client/model/ModularItemModel.java)
    public static List<BakedQuad> getQuadsForSprite(int tintIndex, TextureAtlasSprite sprite, Transformation transform, int color) {
        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();

        int uMax = sprite.getWidth();
        int vMax = sprite.getHeight();

        for (int v = 0; v < vMax; v++) {
            builder.add(buildSideQuad(transform, Direction.UP, tintIndex, color, sprite, 0, v, uMax));
            builder.add(buildSideQuad(transform, Direction.DOWN, tintIndex, color, sprite, 0, v + 1, uMax));
        }

        for (int u = 0; u < uMax; u++) {
            builder.add(buildSideQuad(transform, Direction.EAST, tintIndex, color, sprite, u + 1, 0, vMax));
            builder.add(buildSideQuad(transform, Direction.WEST, tintIndex, color, sprite, u, 0, vMax));
        }

        // front
        builder.add(buildQuad(transform, Direction.NORTH, sprite, tintIndex, color,
                0, 0, 7.5f / 16f, sprite.getU0(), sprite.getV1(),
                0, 1, 7.5f / 16f, sprite.getU0(), sprite.getV0(),
                1, 1, 7.5f / 16f, sprite.getU1(), sprite.getV0(),
                1, 0, 7.5f / 16f, sprite.getU1(), sprite.getV1()
        ));
        // back
        builder.add(buildQuad(transform, Direction.SOUTH, sprite, tintIndex, color,
                0, 0, 8.5f / 16f, sprite.getU0(), sprite.getV1(),
                1, 0, 8.5f / 16f, sprite.getU1(), sprite.getV1(),
                1, 1, 8.5f / 16f, sprite.getU1(), sprite.getV0(),
                0, 1, 8.5f / 16f, sprite.getU0(), sprite.getV0()
        ));

        return builder.build();
    }

    @SuppressWarnings({"MethodWithTooManyParameters", "OverlyLongMethod"})
    private static BakedQuad buildSideQuad(Transformation transform, Direction side, int tintIndex, int color, TextureAtlasSprite sprite, int u, int v, int size) {
        int width = sprite.getWidth();
        int height = sprite.getHeight();

        float x0 = (float) u / width;
        float y0 = (float) v / height;

        float x1 = x0;
        float y1 = y0;

        float z0 = 7.5f / 16f;
        float z1 = 8.5f / 16f;

        switch (side) {
            case WEST:
                z0 = 8.5f / 16f;
                z1 = 7.5f / 16f;

                y1 = (float) (v + size) / height;
                break;
            case EAST:
                y1 = (float) (v + size) / height;
                break;
            case DOWN:
                z0 = 8.5f / 16f;
                z1 = 7.5f / 16f;

                x1 = (float) (u + size) / width;
                break;
            case UP:
                x1 = (float) (u + size) / width;
                break;
            default:
                throw new IllegalArgumentException("can't handle z-oriented side");
        }

        final float eps = 1e-2f;
        float dx = side.getNormal().getX() * eps / width;
        float dy = side.getNormal().getY() * eps / height;

        float u0 = 16f * (x0 - dx);
        float u1 = 16f * (x1 - dx);
        float v0 = 16f * (1f - y0 - dy);
        float v1 = 16f * (1f - y1 - dy);

        return buildQuad(
                transform, remap(side), sprite, tintIndex, color,
                x0, y0, z0, sprite.getU(u0), sprite.getV(v0),
                x1, y1, z0, sprite.getU(u1), sprite.getV(v1),
                x1, y1, z1, sprite.getU(u1), sprite.getV(v1),
                x0, y0, z1, sprite.getU(u0), sprite.getV(v0)
        );
    }

    private static Direction remap(Direction side) {
        // getOpposite is related to the swapping of V direction
        return side.getAxis() == Direction.Axis.Y ? side.getOpposite() : side;
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    private static BakedQuad buildQuad(Transformation transform, Direction side, TextureAtlasSprite sprite, int tintIndex, int color,
                                       float x0, float y0, float z0, float u0, float v0,
                                       float x1, float y1, float z1, float u1, float v1,
                                       float x2, float y2, float z2, float u2, float v2,
                                       float x3, float y3, float z3, float u3, float v3) {
        BakedQuadBuilder builder = new BakedQuadBuilder(sprite);

        builder.setQuadTint(tintIndex);
        builder.setQuadOrientation(side);

        boolean hasTransform = !transform.isIdentity();
        IVertexConsumer consumer = hasTransform ? new TRSRTransformer(builder, transform) : builder;

        putVertex(consumer, side, x0, y0, z0, u0, v0, color);
        putVertex(consumer, side, x1, y1, z1, u1, v1, color);
        putVertex(consumer, side, x2, y2, z2, u2, v2, color);
        putVertex(consumer, side, x3, y3, z3, u3, v3, color);

        return builder.build();
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    private static void putVertex(IVertexConsumer consumer, Direction side, float x, float y, float z, float u, float v, int color) {
        VertexFormat format = consumer.getVertexFormat();
        for (int e = 0; e < format.getElements().size(); e++) {
            switch (format.getElements().get(e).getUsage()) {
                case POSITION:
                    consumer.put(e, x, y, z, 1f);
                    break;
                case COLOR:
                    float a = ((color >> 24) & 0xFF) / 255f; // alpha
                    // reset alpha to 1 if it's 0 to avoid mistakes & make things cleaner
                    a = a == 0 ? 1 : a;

                    float r = ((color >> 16) & 0xFF) / 255f; // red
                    float g = ((color >> 8) & 0xFF) / 255f; // green
                    float b = ((color) & 0xFF) / 255f; // blue

                    consumer.put(e, r, g, b, a);
                    break;
                case NORMAL:
                    float offX = (float) side.getStepX();
                    float offY = (float) side.getStepY();
                    float offZ = (float) side.getStepZ();
                    consumer.put(e, offX, offY, offZ, 0f);
                    break;
                case UV:
                    if (format.getElements().get(e).getIndex() == 0) {
                        consumer.put(e, u, v, 0f, 1f);
                        break;
                    }
                    // else fallthrough to default
                default:
                    consumer.put(e);
                    break;
            }
        }
    }
}
