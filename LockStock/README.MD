LockStock - Block locking plugin for bukkit

License: GPL v3

Permissions
lockstock.use - player can use plugin
lockstock.lock - player can create new locks
lockstock.info.others - player can do /ls info on other players locks



lockstock.admin.open - player can open chests not owned by them
lockstock.admin.delete - player can delete chests not owned by them
lockstock.admin.modify - player can modify chests not owned by them
lockstock.admin.changeowner - player can change owner of lock with /ls changeowner
lockstock.admin.clearplayer - can do /lsa clearplayer
lockstock.admin.listplayer - can do /lsa listplayer
lockstock.admin.import - can do /lsa importfromsqlite
lockstock.admin.updatename - can do /lsa updatename



Commnads:

/lockstock - user commands
alias: /ls

/lockstockadmin - admin commands
alias: /lsa

/lock - shortform for /ls lock

/unlock - shortfrom for /ls unlock

Password locks are called encryptions

Create a password lock:
/ls encrypt s3cret

Unlock with password:

/ls decrypt s3cret

Change password:

/ls recrypt n3v3rkn0w
