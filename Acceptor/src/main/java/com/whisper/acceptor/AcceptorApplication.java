package com.whisper.acceptor;

import com.whisper.common.PrepareResponse;
import com.whisper.common.Proposal;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;


@SpringBootApplication
@RestController
@RequestMapping("/acceptor")
public class AcceptorApplication {
    private Integer promisedN; // 已承诺的最大提案编号
    private Proposal acceptedProposal; // 已接受的提案
    private final int acceptorId; // 节点ID（由环境变量注入）

    // 构造函数：从环境变量获取节点ID
    public AcceptorApplication() {
        this.acceptorId = Integer.parseInt(System.getenv().getOrDefault("ACCEPTOR_ID", "1"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AcceptorApplication.class, args);
    }

    /**
     * Prepare阶段接口
     * @param n 提案编号
     * @return 响应：{success: boolean, acceptedProposal: Proposal}
     */
    @PostMapping("/prepare")
    public PrepareResponse prepare(@RequestParam int n) {
        boolean success = false;
        if (promisedN == null || n > promisedN) {
            promisedN = n;
            success = true;
        }
        return new PrepareResponse(success, acceptedProposal);
    }

    /**
     * Accept阶段接口
     * @param proposal 提案
     * @return 是否接受
     */
    @PostMapping("/accept")
    public boolean accept(@RequestBody Proposal proposal) {
        if (promisedN == null || proposal.getN() >= promisedN) {
            acceptedProposal = proposal;
            return true;
        }
        return false;
    }

    /**
     * 获取已接受的提案（供Learner查询）
     */
    @GetMapping("/accepted")
    public Proposal getAcceptedProposal() {
        return acceptedProposal;
    }


}