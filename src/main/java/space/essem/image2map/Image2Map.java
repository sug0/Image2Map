package space.essem.image2map;

import net.fabricmc.api.ModInitializer;

import space.essem.image2map.renderer.MapRenderer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class Image2Map implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("Loading Image2Map...");

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("mapcreate")
                    .then(CommandManager.argument("path", StringArgumentType.string()).executes(context -> {
                        ServerCommandSource source = context.getSource();
                        Vec3d pos = source.getPosition();
                        PlayerEntity player = source.getPlayer();
                        String input = StringArgumentType.getString(context, "path");

                        source.sendFeedback(new LiteralText("Generating image map..."), false);
                        BufferedImage image;
                        try {
                            if (isValid(input)) {
                                URL url = new URL(input);
                                image = ImageIO.read(url);
                            } else {
                                File file = new File(input);
                                image = ImageIO.read(file);
                            }
                        } catch (IOException e) {
                            source.sendFeedback(new LiteralText("That doesn't seem to be a valid image."), false);
                            return 0;
                        }

                        if (image == null) {
                            source.sendFeedback(new LiteralText("That doesn't seem to be a valid image."), false);
                            return 0;
                        }

                        ItemStack stack = MapRenderer.render(image, source.getWorld(), pos.x, pos.z, player);

                        source.sendFeedback(new LiteralText("Done!"), false);
                        if (!player.inventory.insertStack(stack)) {
                            ItemEntity itemEntity = new ItemEntity(player.world, player.getPos().x, player.getPos().y,
                                    player.getPos().z, stack);
                            player.world.spawnEntity(itemEntity);
                        }

                        return 1;
                    })));
        });
    }

    private static boolean isValid(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
