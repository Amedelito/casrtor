package es.normsafio;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class NormsafioPlugin extends JavaPlugin implements Listener {
    private NamespacedKey typeKey, modelKey, ownerKey;
    private final Map<UUID, Long> balances = new HashMap<>();
    private File balanceFile;
    private final Map<UUID, MenuType> openMenus = new HashMap<>();
    private enum MenuType { SHOP, WELL }
    private static final int CMD_VENDING = 4010, CMD_WELL = 4011, CMD_KART = 4012;

    @Override public void onEnable() {
        typeKey = new NamespacedKey(this, "object_type"); modelKey = new NamespacedKey(this, "custom_model"); ownerKey = new NamespacedKey(this, "owner");
        balanceFile = new File(getDataFolder(), "balances.yml"); if (!getDataFolder().exists()) getDataFolder().mkdirs();
        loadBalances(); getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Normsafio 2.1 activado. Objetos: máquina, pozo y kart.");
    }
    @Override public void onDisable() { saveBalances(); }

    private void loadBalances() {
        if (!balanceFile.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(balanceFile);
        for (String id : y.getKeys(false)) try { balances.put(UUID.fromString(id), y.getLong(id)); } catch (IllegalArgumentException ignored) {}
    }
    private void saveBalances() {
        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
        balances.forEach((id, amount) -> y.set(id.toString(), amount));
        try { y.save(balanceFile); } catch (IOException e) { getLogger().severe("No se pudo guardar balances.yml: " + e.getMessage()); }
    }
    private long balance(Player p) { return balances.getOrDefault(p.getUniqueId(), 0L); }
    private void setBalance(Player p, long value) { balances.put(p.getUniqueId(), Math.max(0, value)); saveBalances(); }

    private ItemStack customItem(Material base, String name, int cmd, String type, List<String> lore) {
        ItemStack item = new ItemStack(base); ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD)); meta.lore(lore.stream().map(s -> Component.text(s, NamedTextColor.GRAY)).toList());
        meta.setCustomModelData(cmd); meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
        meta.getPersistentDataContainer().set(modelKey, PersistentDataType.INTEGER, cmd); item.setItemMeta(meta); return item;
    }
    private ItemStack vendingItem() { return customItem(Material.PAPER,"Máquina expendedora",CMD_VENDING,"vending",List.of("Colócala para abrir la tienda", "Clic derecho: comprar objetos")); }
    private ItemStack wellItem() { return customItem(Material.PAPER,"Pozo de deditas",CMD_WELL,"well",List.of("Colócalo para gestionar tus deditas", "Clic derecho: ingresar o retirar")); }
    private ItemStack kartItem() { return customItem(Material.PAPER,"Kart deportivo",CMD_KART,"kart",List.of("Clic derecho en el suelo para desplegarlo", "Acércate y haz clic derecho para conducir")); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("deditas")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Este comando es para jugadores."); return true; }
            if (args.length == 0) { p.sendMessage(Component.text("Tienes " + balance(p) + " deditas.", NamedTextColor.GOLD)); return true; }
            if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("normsafio.admin")) {
                Player target = Bukkit.getPlayerExact(args[1]); try { long n = Long.parseLong(args[2]); if (target == null || n < 1) throw new NumberFormatException(); setBalance(target, balance(target)+n); target.sendMessage(Component.text("Has recibido "+n+" deditas.", NamedTextColor.GREEN)); sender.sendMessage("Deditas entregadas."); } catch (NumberFormatException ex) { sender.sendMessage("Uso: /deditas give <jugador> <cantidad>"); } return true;
            }
            p.sendMessage("Uso: /deditas [give jugador cantidad] "); return true;
        }
        if (command.getName().equalsIgnoreCase("normsafio")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Este comando es para jugadores."); return true; }
            if (args.length == 0 || args[0].equalsIgnoreCase("items")) {
                if (!p.hasPermission("normsafio.admin")) { p.sendMessage(Component.text("Solo administradores pueden obtener los objetos de prueba.", NamedTextColor.RED)); return true; }
                p.getInventory().addItem(vendingItem(), wellItem(), kartItem()); p.sendMessage(Component.text("Objetos 3D añadidos al inventario.", NamedTextColor.GREEN)); return true;
            }
            p.sendMessage("Uso: /normsafio items (admin)"); return true;
        }
        return false;
    }

    @EventHandler public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getItem() == null) return;
        ItemMeta meta = e.getItem().getItemMeta(); if (meta == null) return;
        String type = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING); if (type == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        e.setCancelled(true); Player p = e.getPlayer();
        if (type.equals("kart")) { if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) spawnKart(p, e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(.5,0,.5)); else spawnKart(p, p.getLocation().add(p.getLocation().getDirection().normalize().multiply(2))); consumeOne(p); return; }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Location loc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation(); if (!loc.getBlock().isEmpty()) return;
        spawnProp(p, loc, type); consumeOne(p);
    }
    private void consumeOne(Player p) { ItemStack hand = p.getInventory().getItemInMainHand(); if (hand.getAmount() <= 1) p.getInventory().setItemInMainHand(null); else hand.setAmount(hand.getAmount()-1); }

    private void spawnProp(Player p, Location blockLoc, String type) {
        World w = blockLoc.getWorld(); if (w == null) return;
        ItemDisplay display = w.spawn(blockLoc.clone().add(.5,.02,.5), ItemDisplay.class, d -> {
            d.setItemStack(type.equals("vending") ? vendingItem() : wellItem()); d.setBillboard(Display.Billboard.FIXED); d.setPersistent(true); d.addScoreboardTag("normsafio_prop");
        });
        Interaction hit = w.spawn(blockLoc.clone().add(.5,.5,.5), Interaction.class, i -> { i.setInteractionWidth(0.95f); i.setInteractionHeight(1.8f); i.setResponsive(true); i.setPersistent(true); i.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type); i.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, display.getUniqueId().toString()); i.addScoreboardTag("normsafio_interaction"); });
        display.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
        p.sendMessage(Component.text(type.equals("vending") ? "Máquina colocada. Clic derecho para abrir la tienda." : "Pozo colocado. Clic derecho para gestionar deditas.", NamedTextColor.GREEN));
    }
    private void spawnKart(Player p, Location loc) {
        World w = loc.getWorld(); if (w == null) return;
        Minecart cart = w.spawn(loc, Minecart.class); cart.setCustomName("Kart de " + p.getName()); cart.setCustomNameVisible(false); cart.setPersistent(true); cart.addScoreboardTag("normsafio_kart");
        ItemDisplay body = w.spawn(loc.clone().add(0, .2, 0), ItemDisplay.class, d -> { d.setItemStack(kartItem()); d.setBillboard(Display.Billboard.FIXED); d.setPersistent(true); d.addScoreboardTag("normsafio_kart_model"); });
        body.setRotation(loc.getYaw(), 0); cart.addPassenger(body); p.sendMessage(Component.text("Kart desplegado. Haz clic derecho en el carrito para subirte.", NamedTextColor.GREEN));
    }
    @EventHandler public void onEntityUse(PlayerInteractAtEntityEvent e) {
        Entity entity = e.getRightClicked(); Player p = e.getPlayer();
        if (entity instanceof Interaction interaction) {
            String type = interaction.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING); if (type == null) return;
            e.setCancelled(true); if (type.equals("vending")) openShop(p); else if (type.equals("well")) openWell(p); return;
        }
        if (entity instanceof Minecart cart && cart.getScoreboardTags().contains("normsafio_kart")) { e.setCancelled(true); if (cart.getPassengers().contains(p)) cart.removePassenger(p); else if (cart.getPassengers().isEmpty()) cart.addPassenger(p); }
    }
    @EventHandler public void onKartDamage(EntityDamageEvent e) { if (e.getEntity().getScoreboardTags().contains("normsafio_kart") || e.getEntity().getScoreboardTags().contains("normsafio_kart_model")) e.setCancelled(true); }

    private void openShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Máquina expendedora · Deditas", NamedTextColor.GOLD));
        inv.setItem(10, shopEntry(Material.IRON_CHESTPLATE,"Armadura de mazmorra",25,"armor"));
        inv.setItem(12, shopEntry(Material.DIAMOND_SWORD,"Espada personalizada",40,"sword"));
        inv.setItem(14, shopEntry(Material.GOLDEN_APPLE,"Manzana dorada",8,"apple"));
        inv.setItem(16, shopEntry(Material.EMERALD,"+5 deditas (prueba) no disponible",999999,"none"));
        inv.setItem(22, named(Material.BARRIER,"Cerrar tienda")); openMenus.put(p.getUniqueId(), MenuType.SHOP); p.openInventory(inv);
    }
    private ItemStack shopEntry(Material mat, String name, int price, String id) { ItemStack it = named(mat,name); ItemMeta m=it.getItemMeta(); m.lore(List.of(Component.text("Precio: "+price+" deditas",NamedTextColor.YELLOW),Component.text("Clic para comprar",NamedTextColor.GRAY))); m.getPersistentDataContainer().set(typeKey,PersistentDataType.STRING,"buy:"+id+":"+price); it.setItemMeta(m); return it; }
    private ItemStack named(Material mat, String name) { ItemStack i=new ItemStack(mat); ItemMeta m=i.getItemMeta(); m.displayName(Component.text(name,NamedTextColor.GOLD)); i.setItemMeta(m); return i; }
    private void openWell(Player p) {
        Inventory inv = Bukkit.createInventory(null,27,Component.text("Pozo de deditas",NamedTextColor.AQUA));
        inv.setItem(11,named(Material.LIME_DYE,"Ingresar 10 deditas")); inv.setItem(15,named(Material.RED_DYE,"Retirar 10 deditas")); inv.setItem(13,named(Material.GOLD_INGOT,"Saldo: "+balance(p)+" deditas")); inv.setItem(22,named(Material.BARRIER,"Cerrar")); openMenus.put(p.getUniqueId(),MenuType.WELL); p.openInventory(inv);
    }
    @EventHandler public void onMenuClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return; MenuType menu=openMenus.get(p.getUniqueId()); if(menu==null) return; e.setCancelled(true); if(e.getCurrentItem()==null) return;
        int slot=e.getRawSlot(); if(slot>=e.getView().getTopInventory().getSize()) return;
        if(slot==22 || e.getCurrentItem().getType()==Material.BARRIER) { p.closeInventory(); openMenus.remove(p.getUniqueId()); return; }
        if(menu==MenuType.WELL) {
            if(slot==11) { if(balance(p)>=10) { setBalance(p,balance(p)-10); p.getInventory().addItem(new ItemStack(Material.AMETHYST_SHARD,10)); p.sendMessage("Has cambiado 10 deditas por 10 fragmentos de amatista (representación de prueba)."); } else p.sendMessage("No tienes 10 deditas."); }
            else if(slot==15) { if(p.getInventory().containsAtLeast(new ItemStack(Material.AMETHYST_SHARD),10)) { p.getInventory().removeItem(new ItemStack(Material.AMETHYST_SHARD,10)); setBalance(p,balance(p)+10); p.sendMessage("Has ingresado 10 deditas al pozo."); } else p.sendMessage("Necesitas 10 fragmentos de amatista para ingresar."); }
            openWell(p); return;
        }
        ItemMeta m=e.getCurrentItem().getItemMeta(); if(m==null) return; String raw=m.getPersistentDataContainer().get(typeKey,PersistentDataType.STRING); if(raw==null||!raw.startsWith("buy:")) return;
        String[] parts=raw.split(":"); if(parts.length<3) return; int price; try{price=Integer.parseInt(parts[2]);}catch(NumberFormatException ex){return;} if(price>balance(p)){p.sendMessage("No tienes suficientes deditas.");return;}
        ItemStack reward=switch(parts[1]) { case "armor" -> new ItemStack(Material.IRON_CHESTPLATE); case "sword" -> new ItemStack(Material.DIAMOND_SWORD); case "apple" -> new ItemStack(Material.GOLDEN_APPLE); default -> null; };
        if(reward==null)return; setBalance(p,balance(p)-price); p.getInventory().addItem(reward); p.sendMessage(Component.text("Compra realizada por "+price+" deditas.",NamedTextColor.GREEN)); openShop(p);
    }
    @EventHandler public void onClose(org.bukkit.event.inventory.InventoryCloseEvent e) { openMenus.remove(e.getPlayer().getUniqueId()); }
}
