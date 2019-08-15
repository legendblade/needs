Needs, Wants, and Desires
=====
> 'Needs' for short

A mod for Minecraft 1.14.4 designed around enabling modpack makers and players to add in
arbitrary stats and stat systems that are as fully configurable as possible.

For more information from a user's or packmaker's perspective go to the mod's online documentation
at https://legendblade.github.com/needs. The rest of this document will be focused on development
and integration from a mod maker's perspective.


Integrating
-----
In the current 1.0.0 version, the best way to integrate for each type of object is:

* **Needs**: Extend `org.winterblade.minecraft.mods.needs.api.needs.Need` (or `ReadOnlyNeed`, or `CachedTickingNeed`
depending on your requirements), and register it with `org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry`.
* **Manipulators**: Extend `org.winterblade.minecraft.mods.needs.api.BaseManipulator` and register it with
`org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry`
* **Mixins**: Extend `org.winterblade.minecraft.mods.needs.api.mixins.BaseMixin` and register it with
`org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry`
* **Level Actions**: Extend `org.winterblade.minecraft.mods.needs.api.LevelAction` and register it with
`org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry`.

There are generally other options for each - every one except Need has an interface you may choose to implement instead -
however, extending the base class is preferable when possible as it will help insulate you from any API changes.


### Lifecycles
Each of the four classes has a series of lifecycle events, including:


#### validate
Currently, this is called when the type has finished being deserialized in order to verify that it has all of the information
necessary to perform its function. When possible, as much that can be validated here should be validated, however, you
should not do any significant object creation here, as validation will generally only be called once and not disposed
of between worlds.

> In the development roadmap, support is planned for the ability to define multiple levels of overrides
in a tier system - any needs/other actions defined in mods, things defined at the pack level in `config`, and things
defined in the `defaultconfig`. Validation may move to a later state once all overrides are done being applied to
the object.


#### onLoaded
The `onLoaded` lifecycle stage will be called when the server world has finished loading, iterating each need, and
working through `manipulators`, `mixins`, each `level` and their associated `level actions`, and finally the `need`
itself, in that order. At this point you should store any references to things that you will need.

In addition after their respective `onLoaded` methods are invoked, every `manipulator`, `mixin`, and `level` will be
automatically registered on the Forge `EVENT_BUS` if they have any methods annotated with `@SubscribeEvent`.


#### onUnloaded
The `onUnloaded` lifecycle stage will be called when the server world is done unloading, in the same order as above.
All objects listed above, as well as all `level actions`, will be unregistered from the `EVENT_BUS`. Any references
stored should be cleaned up here, and the object should generally be placed back in a state where `onLoaded` can be
called again - when in a singleplayer world, this will happen if a user switches worlds.


### Networking
In an effort to minimize network traffic, only needs which are actually necessary to be synced with the client are
synced. If implementing something that needs the information from the need on the client side, call `enableSyncing`
on the need.

> In the current development roadmap, a new interface is planned to denote needs that are already available on the client
side in order to further cut down on network traffic. Please look forward to it.


#### Local Cache
On the client side / thread, no calculation should be done using the base need - a `LocalCachedNeed` will be added to
the `NeedRegistry` and can be obtained through the name of the need by calling `getLocalNeed`, or the entire cache
can be retrieved using `getLocalCache`. These will already have the current value, min, max, and level of the associated
need calculated out.

Additionally, they contain a weak reference to the need and associated level (though at this point, it is unlikely that
either will be invalidated, you should not hold a reference to either and should assume that their value can be
null at any point).
