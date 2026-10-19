package com.solara.protecciones;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private java.sql.Connection conexion;
    private NamespacedKey llaveProteccion;
    
    private HashMap<String, Boolean> permisosMobs = new HashMap<>();
    private HashMap<String, Boolean> permisosTiendas = new HashMap<>();
    private HashMap<String, Boolean> permisosPvP = new HashMap<>();

    @Override
    public void onEnable() {
        this.llaveProteccion = new NamespacedKey(this, "solara_bloque_autentico");
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("darprotesolara").setExecutor(this);
        
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            Class.forName("org.sqlite.JDBC");
            conexion = java.sql.DriverManager.getConnection("jdbc:sqlite:" + new java.io.File(getDataFolder(), "datos.db"));
            
            java.sql.Statement stmt = conexion.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS terrenos (mundo TEXT, x INT, y INT, z INT, dueno TEXT, tamano INT)");
            stmt.close();
            
            getLogger().info("¡Base de datos SQLite y GUI Panel vinculados con exito!");
        } catch (Exception e) {
            getLogger().severe("Error al iniciar la base de datos: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("solara.admin")) {
            p.sendMessage(ChatColor.RED + "No tienes rango para usar este comando secreto.");
            return true;
        }

        if (args.length < 1) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /darprotesolara [10|25|45|88|145|235|400|800]");
            return true;
        }

        String tipo = args[0];
        ItemStack bloqueEspecial = null;
        Material material = Material.AIR;
        String nombreBloque = "";
        String valorNBT = "";

        switch (tipo) {
            case "10": material = Material.WAXED_COPPER_BLOCK; nombreBloque = "§6Cobre Encerado de Protección §7(10x10)"; valorNBT = "pro_10"; break;
            case "25": material = Material.IRON_BLOCK; nombreBloque = "§fBloque de Hierro de Protección §7(25x25)"; valorNBT = "pro_25"; break;
            case "45": material = Material.LAPIS_BLOCK; nombreBloque = "§9Bloque de Lapislázuli de Protección §7(45x45)"; valorNBT = "pro_45"; break;
            case "88": material = Material.GOLD_BLOCK; nombreBloque = "§eBloque de Oro de Protección §7(88x88)"; valorNBT = "pro_88"; break;
            case "145": material = Material.DIAMOND_BLOCK; nombreBloque = "§bBloque de Diamante de Protección §7(145x145)"; valorNBT = "pro_145"; break;
            case "235": material = Material.EMERALD_BLOCK; nombreBloque = "§aBloque de Esmeralda de Protección §7(235x235)"; valorNBT = "pro_235"; break;
            case "400": material = Material.NETHERITE_BLOCK; nombreBloque = "§5Bloque de Netherite de Protección §7(400x400)"; valorNBT = "pro_400"; break;
            case "800": material = Material.RESPAWN_ANCHOR; nombreBloque = "§d⚡ Núcleo de Neorita de Protección ⚡ §7(800x800)"; valorNBT = "pro_800"; break;
            default: p.sendMessage(ChatColor.RED + "Ese tamaño de bloque no existe."); return true;
        }

        bloqueEspecial = new ItemStack(material);
        ItemMeta meta = bloqueEspecial.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nombreBloque);
            List<String> lore = new ArrayList<>();
            lore.add("§7Colócalo para proteger tu terreno.");
            lore.add("§c¡Protección 100% Anti-Hacks habilitada!");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(llaveProteccion, PersistentDataType.STRING, valorNBT);
            bloqueEspecial.setItemMeta(meta);
        }
        p.getInventory().addItem(bloqueEspecial);
        p.sendMessage(ChatColor.GREEN + "¡Se te ha entregado el bloque de protección con éxito!");
        return true;
    }
    @EventHandler
    public void alDarClicDerecho(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            Player p = e.getPlayer();
            
            String dueno = obtenerDuenoTerreno(b);
            if (dueno != null && dueno.equalsIgnoreCase(p.getName())) {
                e.setCancelled(true);
                abrirPanelProteccion(p, b);
            }
        }
    }

    private void abrirPanelProteccion(Player p, Block b) {
        Inventory gui = Bukkit.createInventory(null, 45, "§8Tus Protecciones - Solara");

        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§l⭐ Tus Protecciones ⭐");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Propietario: §e" + p.getName());
        infoLore.add("§7Coordenadas: §aX:" + b.getX() + " Y:" + b.getY() + " Z:" + b.getZ());
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(4, info);

        String idZon = b.getX() + "_" + b.getZ();
        boolean mobsPermitido = permisosMobs.getOrDefault(idZon, false);
        ItemStack btnMobs = new ItemStack(Material.IRON_SWORD);
        ItemMeta mobMeta = btnMobs.getItemMeta();
        mobMeta.setDisplayName("§e§lUso: Matar Mobs");
        List<String> mobLore = new ArrayList<>();
        mobLore.add("§7Permite a visitantes matar criaturas.");
        mobLore.add(mobsPermitido ? "§aEstado: PERMITIDO" : "§cEstado: NO PERMITIDO");
        mobMeta.setLore(mobLore);
        btnMobs.setItemMeta(mobMeta);
        gui.setItem(19, btnMobs);

        boolean tiendaPermitido = permisosTiendas.getOrDefault(idZon, false);
        ItemStack btnTienda = new ItemStack(Material.EMERALD);
        ItemMeta tieMeta = btnTienda.getItemMeta();
        tieMeta.setDisplayName("§e§lUso: Compra y Venta");
        List<String> tieLore = new ArrayList<>();
        tieLore.add("§7Permite a otros usar tus cofres-tienda.");
        tieLore.add(tiendaPermitido ? "§aEstado: PERMITIDO" : "§cEstado: NO PERMITIDO");
        tieMeta.setLore(tieLore);
        btnTienda.setItemMeta(tieMeta);
        gui.setItem(21, btnTienda);

        boolean pvpPermitido = permisosPvP.getOrDefault(idZon, false);
        ItemStack btnPvP = new ItemStack(pvpPermitido ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta pvpMeta = btnPvP.getItemMeta();
        pvpMeta.setDisplayName("§c§l⚠️ Zona de Peligro: PvP ⚠️");
        List<String> pvpLore = new ArrayList<>();
        pvpLore.add("§7Activa o desactiva las peleas adentro.");
        pvpLore.add("§7(No podrán robar ni abrir cofres).");
        pvpLore.add(pvpPermitido ? "§a§lACTIVAR PvP EN PROTECCIÓN" : "§c§lNO ACTIVAR PvP EN PROTE");
        pvpLore.add("§7§oPor defecto viene bloqueado y prohibido.");
        pvpMeta.setLore(pvpLore);
        btnPvP.setItemMeta(pvpMeta);
        gui.setItem(25, btnPvP);
        ItemStack btnPermisosFull = new ItemStack(Material.CHEST);
        ItemMeta pfMeta = btnPermisosFull.getItemMeta();
        pfMeta.setDisplayName("§bAgregar permisos en proteccion a jugador");
        List<String> pfLore = new ArrayList<>(); pfLore.add("§7Otorga acceso completo a romper,"); pfLore.add("§7construir y abrir cofres.");
        pfMeta.setLore(pfLore); btnPermisosFull.setItemMeta(pfMeta);
        gui.setItem(38, btnPermisosFull);

        ItemStack btnPermisosVisita = new ItemStack(Material.OAK_SIGN);
        ItemMeta pvMeta = btnPermisosVisita.getItemMeta();
        pvMeta.setDisplayName("§bActivar permisos de visita");
        List<String> pvLore = new ArrayList<>(); pvLore.add("§7Permite entrar e interactuar"); pvLore.add("§cSIN poder abrir tus cofres.");
        pvMeta.setLore(pvLore); btnPermisosVisita.setItemMeta(pvMeta);
        gui.setItem(40, btnPermisosVisita);

        p.openInventory(gui);
    }

    @EventHandler
    public void alHacerClicGUI(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Tus Protecciones - Solara")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;

            Player p = (Player) e.getWhoClicked();
            int slot = e.getSlot();
            String idZon = p.getLocation().getBlockX() + "_" + p.getLocation().getBlockZ();

            if (slot == 19) {
                boolean actual = permisosMobs.getOrDefault(idZon, false);
                permisosMobs.put(idZon, !actual);
                p.sendMessage(!actual ? "§a[Solara] Mobs: Permitido para visitantes." : "§c[Solara] Mobs: Prohibido para visitantes.");
                p.closeInventory();
            }
            else if (slot == 21) {
                boolean actual = permisosTiendas.getOrDefault(idZon, false);
                permisosTiendas.put(idZon, !actual);
                p.sendMessage(!actual ? "§a[Solara] Tiendas: Interacción permitida." : "§c[Solara] Tiendas: Interacción bloqueada.");
                p.closeInventory();
            }
            else if (slot == 25) {
                boolean actual = permisosPvP.getOrDefault(idZon, false);
                permisosPvP.put(idZon, !actual);
                p.sendMessage(!actual ? "§e§l[⚠️] Activaste el PvP. ¡Te haces responsable de tu seguridad!" : "§a[Solara] PvP Desactivado y protegido.");
                p.closeInventory();
            }
        }
    }

    @EventHandler
    public void alExplotar(EntityExplodeEvent e) {
        if (e.getEntity() instanceof Creeper || e.getEntity() instanceof TNTPrimed) {
            Block b = e.getLocation().getBlock();
            if (estaEnZonaProtegidaCualquiera(b)) {
                e.setCancelled(true);
                e.getLocation().getWorld().sendMessage("§c[Solara-Seguridad] ¡Una explosion fue pasmada y neutralizada en zona protegida!");
            }
        }
    }

    @EventHandler
    public void alPegarJugador(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            Block b = e.getEntity().getLocation().getBlock();
            String idZon = b.getX() + "_" + b.getZ();
            boolean pvpHabilitado = permisosPvP.getOrDefault(idZon, false);

            if (estaEnZonaProtegidaCualquiera(b) && !pvpHabilitado) {
                e.setCancelled(true);
                e.getDamager().sendMessage("§c¡El PvP está desactivado en esta zona protegida!");
            }
        }
    }
    @EventHandler
    public void alColocarBloque(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        Player p = e.getPlayer();
        Block b = e.getBlock();

        if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(llaveProteccion, PersistentDataType.STRING)) {
            ItemMeta meta = item.getItemMeta();
            String tipoProteccion = meta.getPersistentDataContainer().get(llaveProteccion, PersistentDataType.STRING);
            
            if (tipoProteccion == null) { e.setCancelled(true); return; }

            String tamano = ""; int radioNumerico = 0;
            if (tipoProteccion.equals("pro_10")) { tamano = "10x10"; radioNumerico = 10; }
            if (tipoProteccion.equals("pro_25")) { tamano = "25x25"; radioNumerico = 25; }
            if (tipoProteccion.equals("pro_45")) { tamano = "45x45"; radioNumerico = 45; }
            if (tipoProteccion.equals("pro_88")) { tamano = "88x88"; radioNumerico = 88; }
            if (tipoProteccion.equals("pro_145")) { tamano = "145x145"; radioNumerico = 145; }
            if (tipoProteccion.equals("pro_235")) { tamano = "235x235"; radioNumerico = 235; }
            if (tipoProteccion.equals("pro_400")) { tamano = "400x400"; radioNumerico = 400; }
            if (tipoProteccion.equals("pro_800")) { tamano = "800x800"; radioNumerico = 800; }

            try {
                java.sql.PreparedStatement ps = conexion.prepareStatement("INSERT INTO terrenos (mundo, x, y, z, dueno, tamano) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setString(1, b.getWorld().getName()); ps.setInt(2, b.getX()); ps.setInt(3, b.getY()); ps.setInt(4, b.getZ()); ps.setString(5, p.getName()); ps.setInt(6, radioNumerico);
                ps.executeUpdate(); ps.close();
                p.sendMessage(ChatColor.GREEN + "» ¡Bloque auténtico verificado! Has creado una zona protegida de " + tamano + ".");
            } catch (Exception ex) { e.setCancelled(true); }
        } else {
            if (estaDentroDeProteccionAjena(b, p)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void alRomperBloque(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();
        if (estaDentroDeProteccionAjena(b, p)) {
            e.setCancelled(true);
            p.sendBlockChange(b.getLocation(), b.getBlockData());
        }
    }

    private String obtenerDuenoTerreno(Block bloque) {
        try {
            java.sql.PreparedStatement ps = conexion.prepareStatement("SELECT dueno FROM terrenos WHERE mundo = ? AND x = ? AND y = ? AND z = ?");
            ps.setString(1, bloque.getWorld().getName()); ps.setInt(2, bloque.getX()); ps.setInt(3, bloque.getY()); ps.setInt(4, bloque.getZ());
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) { String d = rs.getString("dueno"); rs.close(); ps.close(); return d; }
            rs.close(); ps.close();
        } catch (Exception e) {}
        return null;
    }

    private boolean estaEnZonaProtegidaCualquiera(Block bloque) {
        return estaDentroDeProteccionAjena(bloque, null);
    }

    private boolean estaDentroDeProteccionAjena(Block bloque, Player jugador) {
        try {
            java.sql.PreparedStatement ps = conexion.prepareStatement("SELECT * FROM terrenos WHERE mundo = ?");
            ps.setString(1, bloque.getWorld().getName());
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int proteX = rs.getInt("x"); int proteY = rs.getInt("y"); int proteZ = rs.getInt("z");
                String dueno = rs.getString("dueno"); int radio = rs.getInt("tamano");
                if (jugador != null && dueno.equalsIgnoreCase(jugador.getName())) continue;
                if (bloque.getX() >= (proteX - radio) && bloque.getX() <= (proteX + radio) &&
                    bloque.getY() >= (proteY - radio) && bloque.getY() <= (proteY + radio) &&
                    bloque.getZ() >= (proteZ - radio) && bloque.getZ() <= (proteZ + radio)) {
                    rs.close(); ps.close(); return true;
                }
            }
            rs.close(); ps.close();
        } catch (Exception e) {}
        return false;
    }
}
