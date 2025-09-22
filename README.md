# FakeInventories

FakeInventories is a simple library plugin for Lumi and Nukkit-MOT Minecraft Bedrock server software, that will help you to create
your custom virtual inventories with ease.

### [Download the plugin](https://github.com/LuminiaDev/FakeInventories/releases)

## How to use

```java
FakeInventory inventory = new FakeInventory(InventoryType.CHEST, "Custom inventory");

inventory.setDefaultItemHandler((item, event) -> {
    event.setCancelled(true);

    Player target = event.getTransaction().getSource();
    target.sendMessage("is default item handler");
});

inventory.setItem(5, Item.get(Item.DIAMOND), (item, event) -> {
    event.setCancelled(true);

    Player target = event.getTransaction().getSource();
    target.sendMessage("is custom item handler");
    target.removeWindow(inventory);
});

player.addWindow(inventory);
```

> [!WARNING]
> To open fake inventory from the chat window or form, you need to add a delay of 10-20 ticks so that the window has time to close, otherwise the inventory will not open.

## Maven
Adding repo:
```xml
<repository>
    <id>luminiadev-repository-releases</id>
    <url>https://repo.luminiadev.com/releases</url>
</repository>
```

Adding dependency:
```xml
<dependency>
    <groupId>com.luminiadev.fakeinventories</groupId>
    <artifactId>fakeinventories</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Gradle
Adding repo:
```kts
maven {
    name = "luminiadevRepositoryReleases"
    url = uri("https://repo.luminiadev.com/releases")
}
```

Adding dependency:
```kts
compileOnly("com.luminiadev.fakeinventories:fakeinventories:1.2.0")
```