package net.silentchaos512.gear.client.model.part;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.model.IModelConfiguration;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.material.IMaterialDisplay;
import net.silentchaos512.gear.api.material.MaterialLayer;
import net.silentchaos512.gear.client.model.ModelErrorLogging;
import net.silentchaos512.gear.config.Config;
import net.silentchaos512.gear.gear.material.MaterialInstance;
import net.silentchaos512.gear.gear.part.PartData;
import net.silentchaos512.gear.item.CompoundPartItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CompoundPartModelOverrideList extends ItemOverrides {
    private final Cache<CacheKey, BakedModel> bakedModelCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final CompoundPartModel model;
    private final IModelConfiguration owner;
    private final ModelBakery bakery;
    private final Function<Material, TextureAtlasSprite> spriteGetter;
    private final ModelState modelTransform;
    private final ResourceLocation modelLocation;

    @SuppressWarnings("ConstructorWithTooManyParameters")
    public CompoundPartModelOverrideList(CompoundPartModel model,
                                         IModelConfiguration owner,
                                         ModelBakery bakery,
                                         Function<Material, TextureAtlasSprite> spriteGetter,
                                         ModelState modelTransform,
                                         ResourceLocation modelLocation) {
        this.model = model;
        this.owner = owner;
        this.bakery = bakery;
        this.spriteGetter = spriteGetter;
        this.modelTransform = modelTransform;
        this.modelLocation = modelLocation;
    }

    static boolean isDebugLoggingEnabled() {
        return Config.Common.modelAndTextureLogging.get();
    }

    @Nullable
    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel worldIn, @Nullable LivingEntity entityIn, int p_173469_) {
        CacheKey key = getKey(model, stack, worldIn, entityIn);
        try {
            return bakedModelCache.get(key, () -> getOverrideModel(stack, worldIn, entityIn));
        } catch (Exception e) {
            ModelErrorLogging.notifyOfException(e, "compound part");
        }
        return model;
    }

    private BakedModel getOverrideModel(ItemStack stack, @Nullable ClientLevel worldIn, @Nullable LivingEntity entityIn) {
        List<MaterialLayer> layers = new ArrayList<>();

        PartData part = PartData.from(stack);
        MaterialInstance primaryMaterial = CompoundPartItem.getPrimaryMaterial(stack);
        if (part != null && primaryMaterial != null) {
            addWithBlendedColor(layers, part, primaryMaterial, stack);
        }

        return model.bake(layers, "test", owner, bakery, spriteGetter, modelTransform, this, modelLocation);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private void addWithBlendedColor(List<MaterialLayer> list, PartData part, MaterialInstance material, ItemStack stack) {
        IMaterialDisplay materialModel = material.getDisplayProperties();

        List<MaterialLayer> layers = materialModel.getLayerList(this.model.gearType, part, material).getLayers();
        for (int i = 0; i < layers.size(); i++) {
            MaterialLayer layer = layers.get(i);
            if ((layer.getColor() & 0xFFFFFF) < 0xFFFFFF) {
                int blendedColor = part.getColor(stack, i, 0);
                list.add(new MaterialLayer(layer.getTextureId(), part.getType(), blendedColor, false));
            } else {
                list.add(layer);
            }
        }
    }

    private static CacheKey getKey(BakedModel model, ItemStack stack, @Nullable Level world, @Nullable LivingEntity entity) {
        return new CacheKey(model, CompoundPartItem.getModelKey(stack));
    }

    public void clearCache() {
        if (CompoundPartModelOverrideList.isDebugLoggingEnabled()) {
            SilentGear.LOGGER.debug("Clearing model cache for {}/{}", this.model.partType, this.model.gearType);
        }
        bakedModelCache.invalidateAll();
    }

    static final class CacheKey {
        final BakedModel parent;
        final String data;

        CacheKey(BakedModel parent, String hash) {
            this.parent = parent;
            this.data = hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return parent == cacheKey.parent && Objects.equals(data, cacheKey.data);
        }

        @Override
        public int hashCode() {
            return 31 * parent.hashCode() + data.hashCode();
        }
    }
}
