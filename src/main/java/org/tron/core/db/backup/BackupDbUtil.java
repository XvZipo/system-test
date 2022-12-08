package org.tron.core.db.backup;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.PropUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db2.core.RevokingDBWithCachingNewValue;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;

@Slf4j
@Component
public class BackupDbUtil {

  @Getter
  private static String DB_BACKUP_STATE = "DB";
  private static final int DB_BACKUP_INDEX1 = 1;
  private static final int DB_BACKUP_INDEX2 = 2;

  @Getter
  private static final int DB_BACKUP_STATE_DEFAULT = 11;

  @Setter
  @Getter
  private long lastBackupHeight = 0;

  public enum STATE {
    BAKINGONE(1), BAKEDONE(11), BAKINGTWO(2), BAKEDTWO(22);
    public int status;

    private STATE(int status) {
      this.status = status;
    }

    public int getStatus() {
      return status;
    }

    public static STATE valueOf(int value) {
      switch (value) {
        case 1:
          return BAKINGONE;
        case 11:
          return BAKEDONE;
        case 2:
          return BAKINGTWO;
        case 22:
          return BAKEDTWO;
        default:
          return BAKEDONE;
      }
    }
  }

  @Getter
  @Autowired
  private RevokingDatabase db;

  private Args args = Args.getInstance();

  private int getBackupState() {
    try {
      return Integer.valueOf(PropUtil
          .readProperty(args.getDbBackupConfig().getPropPath(), BackupDbUtil.DB_BACKUP_STATE)
      );
    } catch (NumberFormatException ignore) {
      return DB_BACKUP_STATE_DEFAULT;  //get default state if prop file is newly created
    }
  }

  private void setBackupState(int status) {
    PropUtil.writeProperty(args.getDbBackupConfig().getPropPath(), BackupDbUtil.DB_BACKUP_STATE,
        String.valueOf(status));
  }

  private void switchBackupState() {
    switch (STATE.valueOf(getBackupState())) {
      case BAKINGONE:
        setBackupState(STATE.BAKEDONE.getStatus());
        break;
      case BAKEDONE:
        setBackupState(STATE.BAKEDTWO.getStatus());
        break;
      case BAKINGTWO:
        setBackupState(STATE.BAKEDTWO.getStatus());
        break;
      case BAKEDTWO:
        setBackupState(STATE.BAKEDONE.getStatus());
        break;
      default:
        break;
    }
  }

  public boolean canDoBackup(BlockCapsule block) {
    //not reach frequency
    if (block.getNum() % args.getDbBackupConfig().getFrequency() != 0) {
      //backup pause because of conflict with produce, so remedy it.
      if (block.getNum() - getLastBackupHeight() >= args.getDbBackupConfig().getFrequency()) {
        logger
            .debug("block height: {}, lastbackupheight: {}", block.getNum(), getLastBackupHeight());
        setLastBackupHeight(block.getNum());
        return true;
      }
      return false;
    } else { //reach frequency
      setLastBackupHeight(block.getNum());
      return true;
    }
  }

  public void doBackup(BlockCapsule block) {
    if (!canDoBackup(block)) {
      return;
    }

    long t1 = System.currentTimeMillis();
    try {
      switch (STATE.valueOf(getBackupState())) {
        case BAKINGONE:
          deleteBackup(DB_BACKUP_INDEX1);
          backup(DB_BACKUP_INDEX1);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX2);
          break;
        case BAKEDONE:
          deleteBackup(DB_BACKUP_INDEX2);
          backup(DB_BACKUP_INDEX2);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX1);
          break;
        case BAKINGTWO:
          deleteBackup(DB_BACKUP_INDEX2);
          backup(DB_BACKUP_INDEX2);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX1);
          break;
        case BAKEDTWO:
          deleteBackup(DB_BACKUP_INDEX1);
          backup(DB_BACKUP_INDEX1);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX2);
          break;
        default:
          logger.warn("invalid backup state");
      }
    } catch (RocksDBException e) {
      logger.warn("backup db error:" + e);
    }
    logger.info("current block number is {}, backup all store use {} ms!", block.getNum(),
        System.currentTimeMillis() - t1);
  }

  private void backup(int i) throws RocksDBException {
    String path = "";
    if (i == DB_BACKUP_INDEX1) {
      path = args.getDbBackupConfig().getBak1path();
    } else if (i == DB_BACKUP_INDEX2) {
      path = args.getDbBackupConfig().getBak2path();
    } else {
      throw new RuntimeException("Error backup with undefined index");
    }
    List<RevokingDBWithCachingNewValue> stores = ((SnapshotManager) db).getDbs();
    for (RevokingDBWithCachingNewValue store : stores) {
      if (((SnapshotRoot) (store.getHead().getRoot())).getDb().getClass()
          == org.tron.core.db2.common.RocksDB.class) {
        ((org.tron.core.db2.common.RocksDB) ((SnapshotRoot) (store.getHead().getRoot())).getDb())
            .getDb().backup(path);
      }
    }
  }

  private void deleteBackup(int i) {
    String path = "";
    if (i == DB_BACKUP_INDEX1) {
      path = args.getDbBackupConfig().getBak1path();
    } else if (i == DB_BACKUP_INDEX2) {
      path = args.getDbBackupConfig().getBak2path();
    } else {
      throw new RuntimeException("Error deleteBackup with undefined index");
    }
    List<RevokingDBWithCachingNewValue> stores = ((SnapshotManager) db).getDbs();
    for (RevokingDBWithCachingNewValue store : stores) {
      if (((SnapshotRoot) (store.getHead().getRoot())).getDb().getClass()
          == org.tron.core.db2.common.RocksDB.class) {
        ((org.tron.core.db2.common.RocksDB) (((SnapshotRoot) (store.getHead().getRoot()))
            .getDb()))
            .getDb().deleteDbBakPath(path);
      }
    }
  }
}
