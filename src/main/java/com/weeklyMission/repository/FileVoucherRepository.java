package com.weeklyMission.repository;

import com.weeklyMission.domain.FixedAmountVoucher;
import com.weeklyMission.domain.Voucher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev", "prod"})
@Primary
public class FileVoucherRepository implements VoucherRepository{
    private final String path;
    private final String seperator = ",";
    private final Map<UUID, Voucher> storage;

    public FileVoucherRepository(@Value("${filePath.repository.voucher}") String path) {
        this.path = System.getProperty("user.dir") + path;
        storage = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init(){
        loadFile();
    }

    private void loadFile() {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))){
            String data;
            while((data=br.readLine())!=null){
                String[] dataSplit = data.split(",");
                VoucherFactory voucherFactory = VoucherFactory.of(dataSplit[0]);
                Voucher voucher = voucherFactory.makeVoucher(UUID.fromString(dataSplit[1]),
                    Integer.parseInt(dataSplit[2]));
                storage.put(voucher.getVoucherId(), voucher);
            }
        }catch(IOException e){
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreDestroy
    public void close(){
        writeFile();
    }

    private void writeFile() {
        String type;
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, false), StandardCharsets.UTF_8))){
            for (Voucher voucher : storage.values()) {
                if(voucher instanceof FixedAmountVoucher) type="fixed";
                else type="percent";
                bw.write(type + seperator + voucher.getVoucherId() + seperator + voucher.getAmount());
                bw.newLine();
            }
        }catch (IOException e){
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Voucher createVoucher(Voucher voucher) {
        storage.put(voucher.getVoucherId(), voucher);
        return voucher;
    }

    @Override
    public List<Voucher> getVoucherList() {
        return new ArrayList<>(storage.values());
    }
}
