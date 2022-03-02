package kiwi.hoonkun.plugins.pixel.commands

import kiwi.hoonkun.plugins.pixel.Entry
import kiwi.hoonkun.plugins.pixel.worker.PixelWorker
import org.bukkit.command.CommandSender
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.GitAPIException

class ResetExecutor(private val plugin: Entry): Executor() {

    companion object {

        val COMPLETE_LIST_0 = mutableListOf("<steps>", "<commit_hash>")

    }

    override suspend fun exec(sender: CommandSender?, args: List<String>): CommandExecuteResult {
        if (args.isEmpty())
            return CommandExecuteResult(false, "argument is missing. back steps or commit name must be specified.")

        val repo = Entry.repository ?: return invalidRepositoryResult

        val target = args[1].toIntOrNull()

        try {
            Git(repo).reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef(if (target != null && target <= 10) "HEAD~${args[1]}" else args[1])
                .call()
        } catch (exception: GitAPIException) {
            return createGitApiFailedResult(exception)
        }

        PixelWorker.replaceFromVersionControl(plugin, dimensions(args[0]))

        return CommandExecuteResult(true, "successfully reset commits.")
    }

    override fun autoComplete(args: List<String>): MutableList<String> {
        return when (args.size) {
            1 -> COMPLETE_LIST_DIMENSIONS
            2 -> COMPLETE_LIST_0
            else -> COMPLETE_LIST_EMPTY
        }
    }

}