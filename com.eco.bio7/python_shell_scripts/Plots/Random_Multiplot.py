# An example: several plots of random data in one figure (no selection needed)
import numpy as np
import matplotlib.pyplot as plt

rng = np.random.default_rng()
fig, axes = plt.subplots(2, 3, figsize=(12, 7))
axes[0, 0].boxplot(rng.random(1000))
axes[0, 1].plot(rng.random(1000), ".", markersize=3)
axes[0, 2].imshow(rng.random((10, 10)) * 2500, cmap="viridis")
axes[1, 0].hist(rng.random(100), bins=15)
axes[1, 1].pie(rng.random(10))
ax3 = fig.add_subplot(2, 3, 6, projection="3d")
axes[1, 2].set_visible(False)
x, y = np.meshgrid(np.arange(10), np.arange(10))
ax3.plot_surface(x, y, rng.random((10, 10)) * 2500, cmap="viridis")
fig.tight_layout()
plt.show()
