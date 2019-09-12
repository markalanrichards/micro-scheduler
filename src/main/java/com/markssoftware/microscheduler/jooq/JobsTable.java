package com.markssoftware.microscheduler.jooq;

import com.markssoftware.microscheduler.jobs.JobInfo;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.collector.Collectors2;
import org.jooq.Configuration;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.CustomRecord;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public class JobsTable extends CustomTable<JobsTable.JobsRecord> {
    private final TableField<JobsRecord, byte[]> uuid;
    private final TableField<JobsRecord, String> cron;
    private final TableField<JobsRecord, String> source;

    public JobsTable() {
        super(DSL.name("jobs"));
        uuid = createField(DSL.name("uuid"), SQLDataType.BINARY(16).identity(true).nullable(false));
        cron = createField(DSL.name("cron"), SQLDataType.VARCHAR.nullable(false));
        source = createField(DSL.name("source"), SQLDataType.VARCHAR.nullable(false));
    }

    private static byte[] getBytesFromUUID(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    private static UUID getUUIDFromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();

        return new UUID(high, low);
    }

    @Override
    public Class<? extends JobsRecord> getRecordType() {
        return JobsRecord.class;
    }

    public void save(JobInfo jobInfo, Configuration configuration) {
        DSL.using(configuration).insertInto(this)
                .set(uuid, getBytesFromUUID(jobInfo.getUuid()))
                .set(cron, jobInfo.getCron())
                .set(source, jobInfo.getSource())
                .execute();
    }

    public ImmutableSet<JobInfo> jobs(Configuration configuration) {
        return Arrays.stream(DSL.using(configuration).select(uuid, cron, source).from(this).forUpdate().fetchArray())
                .map(record -> JobInfo.builder()
                        .cron(record.get(cron))
                        .source(record.get(source))
                        .uuid(getUUIDFromBytes(record.get(uuid)))
                        .build())
                .collect(Collectors2.toImmutableSet());
    }

    static class JobsRecord extends CustomRecord<JobsRecord> {

        JobsRecord(Table<JobsRecord> table) {
            super(table);
        }
    }
}
