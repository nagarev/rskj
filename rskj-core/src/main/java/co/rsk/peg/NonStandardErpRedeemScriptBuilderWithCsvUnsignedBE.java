package co.rsk.peg;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.script.ScriptOpCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static co.rsk.peg.ErpRedeemScriptBuilderUtils.removeOpCheckMultisig;

public class NonStandardErpRedeemScriptBuilderWithCsvUnsignedBE implements ErpRedeemScriptBuilder {
    private static final Logger logger = LoggerFactory.getLogger(NonStandardErpRedeemScriptBuilderWithCsvUnsignedBE.class);
    private static final int CSV_BYTES_NEEDED_LENGTH = 2;

    @Override
    public Script createRedeemScriptFromKeys(List<BtcECKey> defaultPublicKeys,
                                             int defaultThreshold,
                                             List<BtcECKey> emergencyPublicKeys,
                                             int emergencyThreshold,
                                             long csvValue) {

        Script defaultRedeemScript = ScriptBuilder.createRedeemScript(defaultThreshold, defaultPublicKeys);
        Script emergencyRedeemScript = ScriptBuilder.createRedeemScript(emergencyThreshold, emergencyPublicKeys);

        ErpRedeemScriptBuilderUtils.validateRedeemScriptValues(defaultRedeemScript, emergencyRedeemScript, csvValue);

        byte[] serializedCsvValue = Utils.unsignedLongToByteArrayBE(csvValue, CSV_BYTES_NEEDED_LENGTH);
        logger.debug("[createRedeemScriptFromKeys] Creating the redeem script from the scripts");
        Script redeemScript = createRedeemScriptFromScripts(defaultRedeemScript, emergencyRedeemScript, serializedCsvValue);
        
        logger.debug("[createRedeemScriptFromKeys] Validating redeem script size");
        ScriptValidations.validateScriptSize(redeemScript);
        
        return redeemScript;

    }

    private Script createRedeemScriptFromScripts(Script defaultRedeemScript,
                                                        Script emergencyRedeemScript,
                                                        byte[] serializedCsvValue) {

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(removeOpCheckMultisig(defaultRedeemScript))
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(removeOpCheckMultisig(emergencyRedeemScript))
            .op(ScriptOpCodes.OP_ENDIF)
            .op(ScriptOpCodes.OP_CHECKMULTISIG)
            .build();
    }
}