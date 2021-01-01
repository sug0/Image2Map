package space.essem.image2map;

import net.fabricmc.api.ModInitializer;

import space.essem.image2map.renderer.MapRenderer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
                    String input = StringArgumentType.getString(context, "path");
                    BufferedImage image = fetchImage(input, source);

                    if (image == null) {
                        return 0;
                    }

                    dropStack(image, source);
                    source.sendFeedback(new LiteralText("Done!"), false);

                    return 1;
                })));
            dispatcher.register(CommandManager.literal("splitmapcreate")
                .then(CommandManager.argument("path", StringArgumentType.string()).executes(context -> {
                    ServerCommandSource source = context.getSource();
                    String input = StringArgumentType.getString(context, "path");
                    BufferedImage image = fetchImage(input, source);

                    if (image == null) {
                        return 0;
                    }

                    final int height = image.getHeight();
                    final int width = image.getWidth();

                    BufferedImage[] frames = new BufferedImage[2];

                    if (height > width) {
                        final int half = height / 2;
                        frames[0] = image.getSubimage(0, 0, width, half);
                        frames[1] = image.getSubimage(0, half, width, half);
                    } else {
                        final int half = width / 2;
                        frames[0] = image.getSubimage(0, 0, half, height);
                        frames[1] = image.getSubimage(half, 0, half, height);
                    }

                    dropStack(frames[0], source);
                    dropStack(frames[1], source);
                    source.sendFeedback(new LiteralText("Done!"), false);

                    return 1;
                })));
        });
    }

    private static void dropStack(BufferedImage image, ServerCommandSource source) {
        try {
            PlayerEntity player = source.getPlayer();
            Vec3d pos = source.getPosition();
            ItemStack stack = MapRenderer.render(image, source.getWorld(), pos.x, pos.z, player);
            if (!player.inventory.insertStack(stack)) {
                ItemEntity itemEntity = new ItemEntity(player.world, player.getPos().x, player.getPos().y,
                        player.getPos().z, stack);
                player.world.spawnEntity(itemEntity);
            }
        } catch (CommandSyntaxException e) {
            source.sendFeedback(new LiteralText("Invalid command."), false);
            return;
        }
    }

    private static BufferedImage fetchImage(String input, ServerCommandSource source) {
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
            return null;
        }

        if (image == null) {
            source.sendFeedback(new LiteralText("That doesn't seem to be a valid image."), false);
            return null;
        }
        return image;
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
